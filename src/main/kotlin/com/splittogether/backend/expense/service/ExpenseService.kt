package com.splittogether.backend.expense.service

import com.splittogether.backend.balance.service.BalanceService
import com.splittogether.backend.common.exception.*
import com.splittogether.backend.common.repository.CurrencyRepository
import com.splittogether.backend.expense.dto.*
import com.splittogether.backend.expense.entity.Expense
import com.splittogether.backend.expense.entity.ExpenseParticipant
import com.splittogether.backend.expense.entity.SplitMethod
import com.splittogether.backend.expense.repository.*
import com.splittogether.backend.group.entity.GroupRole
import com.splittogether.backend.group.entity.GroupStatus
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.group.service.MembershipGuard
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val expenseParticipantRepository: ExpenseParticipantRepository,
    private val splitMethodRepository: SplitMethodRepository,
    private val expenseCategoryRepository: ExpenseCategoryRepository,
    private val groupRepository: GroupRepository,
    private val membershipGuard: MembershipGuard,
    private val userRepository: UserRepository,
    private val currencyRepository: CurrencyRepository,
    private val balanceService: BalanceService
) {

    @Transactional(readOnly = true)
    fun countActiveByGroupId(groupId: Long): Long =
        expenseRepository.countActiveByGroupId(groupId)

    @Transactional(readOnly = true)
    fun countActiveByGroupIds(groupIds: List<Long>): Map<Long, Long> =
        expenseRepository.countActiveByGroupIds(groupIds).associate { it.groupId to it.count }

    private fun requireActiveMember(groupId: Long, userId: Long) =
        membershipGuard.requireActiveMember(groupId, userId)

    private fun Expense.toResponse(participants: List<ExpenseParticipant>) = ExpenseResponse(
        id = id,
        groupId = group.id,
        title = title,
        description = description,
        amount = amount,
        currencyCode = currency.code,
        categoryCode = category?.code,
        splitMethod = splitMethod.code,
        expenseDate = expenseDate,
        paidByUserId = paidBy.id,
        paidByName = paidBy.displayName,
        participants = participants.map {
            ExpenseParticipantResponse(
                userId = it.user.id,
                displayName = it.user.displayName,
                share = it.share,
                weight = it.weight
            )
        },
        createdAt = createdAt
    )

    @Transactional(readOnly = true)
    fun getExpenses(userId: Long, groupId: Long): List<ExpenseResponse> {
        if (!groupRepository.existsById(groupId)) throw GroupNotFoundException("Group not found")
        requireActiveMember(groupId, userId)
        val expenses = expenseRepository.findActiveByGroupId(groupId)
        if (expenses.isEmpty()) return emptyList()
        val participantsByExpense = expenseParticipantRepository
            .findByExpenseIdIn(expenses.map { it.id })
            .groupBy { it.expense.id }
        return expenses.map { expense ->
            expense.toResponse(participantsByExpense[expense.id] ?: emptyList())
        }
    }

    @Transactional(readOnly = true)
    fun getExpense(userId: Long, groupId: Long, expenseId: Long): ExpenseResponse {
        if (!groupRepository.existsById(groupId)) throw GroupNotFoundException("Group not found")
        requireActiveMember(groupId, userId)
        val expense = expenseRepository.findById(expenseId)
            .orElseThrow { ExpenseNotFoundException("Expense not found") }
        if (expense.group.id != groupId || expense.deletedAt != null)
            throw ExpenseNotFoundException("Expense not found")
        return expense.toResponse(expenseParticipantRepository.findByExpenseId(expenseId))
    }

    @Transactional
    fun createExpense(userId: Long, groupId: Long, request: CreateExpenseRequest): ExpenseResponse {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        if (group.status.code != GroupStatus.ACTIVE) throw GroupArchivedException("Group is archived")
        requireActiveMember(groupId, userId)

        val currency = currencyRepository.findByCode(request.currencyCode)
            ?: throw CurrencyNotFoundException("Currency not found: ${request.currencyCode}")
        val splitMethod = splitMethodRepository.findByCode(request.splitMethod.uppercase())
            ?: throw InvalidExpenseException("Unknown split method: ${request.splitMethod}")
        val category = request.categoryCode?.let {
            expenseCategoryRepository.findByCode(it) ?: throw InvalidExpenseException("Unknown category: $it")
        }

        val paidBy = userRepository.findById(request.paidByUserId)
            .orElseThrow { UserNotFoundException("Paid-by user not found") }
        requireActiveMember(groupId, request.paidByUserId)

        val participantUsers = request.participants.map { p ->
            requireActiveMember(groupId, p.userId)
            userRepository.findById(p.userId).orElseThrow { UserNotFoundException("User not found: ${p.userId}") }
        }

        val shares = calculateShares(request.amount, splitMethod.code, request.paidByUserId, request.participants)

        val creator = userRepository.getReferenceById(userId)
        val expense = expenseRepository.save(
            Expense(
                group = group,
                paidBy = paidBy,
                title = request.title,
                description = request.description,
                amount = request.amount,
                currency = currency,
                category = category,
                splitMethod = splitMethod,
                expenseDate = request.expenseDate,
                createdBy = creator
            )
        )

        val savedParticipants = request.participants.mapIndexed { i, p ->
            expenseParticipantRepository.save(
                ExpenseParticipant(
                    expense = expense,
                    user = participantUsers[i],
                    share = shares[p.userId]!!.share,
                    weight = shares[p.userId]!!.weight
                )
            )
        }

        savedParticipants.forEach { participant ->
            if (participant.user.id != request.paidByUserId) {
                balanceService.updateBalance(groupId, participant.user.id, request.paidByUserId, participant.share)
            }
        }

        return expense.toResponse(savedParticipants)
    }

    @Transactional
    fun updateExpense(userId: Long, groupId: Long, expenseId: Long, request: UpdateExpenseRequest): ExpenseResponse {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        if (group.status.code != GroupStatus.ACTIVE) throw GroupArchivedException("Group is archived")
        val member = requireActiveMember(groupId, userId)

        val expense = expenseRepository.findById(expenseId)
            .orElseThrow { ExpenseNotFoundException("Expense not found") }
        if (expense.group.id != groupId || expense.deletedAt != null)
            throw ExpenseNotFoundException("Expense not found")

        if (expense.createdBy.id != userId && member.role.code == GroupRole.MEMBER)
            throw InsufficientPermissionsException("Only the expense creator or group admin/owner can edit expenses")

        val oldPaidById = expense.paidBy.id
        val oldParticipants = expenseParticipantRepository.findByExpenseId(expenseId)

        // Reverse old balance contributions
        oldParticipants.forEach { p ->
            if (p.user.id != oldPaidById) {
                balanceService.updateBalance(groupId, p.user.id, oldPaidById, p.share.negate())
            }
        }

        val currency = currencyRepository.findByCode(request.currencyCode)
            ?: throw CurrencyNotFoundException("Currency not found: ${request.currencyCode}")
        val splitMethod = splitMethodRepository.findByCode(request.splitMethod.uppercase())
            ?: throw InvalidExpenseException("Unknown split method: ${request.splitMethod}")
        val category = request.categoryCode?.let {
            expenseCategoryRepository.findByCode(it) ?: throw InvalidExpenseException("Unknown category: $it")
        }

        val paidBy = userRepository.findById(request.paidByUserId)
            .orElseThrow { UserNotFoundException("Paid-by user not found") }
        requireActiveMember(groupId, request.paidByUserId)

        val participantUsers = request.participants.map { p ->
            requireActiveMember(groupId, p.userId)
            userRepository.findById(p.userId).orElseThrow { UserNotFoundException("User not found: ${p.userId}") }
        }

        val shares = calculateShares(request.amount, splitMethod.code, request.paidByUserId, request.participants)

        expense.paidBy = paidBy
        expense.title = request.title
        expense.description = request.description
        expense.amount = request.amount
        expense.currency = currency
        expense.category = category
        expense.splitMethod = splitMethod
        expense.expenseDate = request.expenseDate
        expenseRepository.save(expense)

        expenseParticipantRepository.deleteByExpenseId(expenseId)
        val newParticipants = request.participants.mapIndexed { i, p ->
            expenseParticipantRepository.save(
                ExpenseParticipant(
                    expense = expense,
                    user = participantUsers[i],
                    share = shares[p.userId]!!.share,
                    weight = shares[p.userId]!!.weight
                )
            )
        }

        newParticipants.forEach { participant ->
            if (participant.user.id != request.paidByUserId) {
                balanceService.updateBalance(groupId, participant.user.id, request.paidByUserId, participant.share)
            }
        }

        return expense.toResponse(newParticipants)
    }

    @Transactional
    fun deleteExpense(userId: Long, groupId: Long, expenseId: Long) {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        if (group.status.code != GroupStatus.ACTIVE) throw GroupArchivedException("Group is archived")
        val member = requireActiveMember(groupId, userId)

        val expense = expenseRepository.findById(expenseId)
            .orElseThrow { ExpenseNotFoundException("Expense not found") }
        if (expense.group.id != groupId || expense.deletedAt != null)
            throw ExpenseNotFoundException("Expense not found")

        if (expense.createdBy.id != userId && member.role.code == GroupRole.MEMBER)
            throw InsufficientPermissionsException("Only the expense creator or group admin/owner can delete expenses")

        val paidById = expense.paidBy.id
        val participants = expenseParticipantRepository.findByExpenseId(expenseId)

        participants.forEach { p ->
            if (p.user.id != paidById) {
                balanceService.updateBalance(groupId, p.user.id, paidById, p.share.negate())
            }
        }

        val deleter = userRepository.getReferenceById(userId)
        expense.deletedAt = Instant.now()
        expense.deletedBy = deleter
        expenseRepository.save(expense)
    }

    // ── share calculation ──────────────────────────────────────────────────────

    private data class ShareCalc(val share: BigDecimal, val weight: BigDecimal? = null)

    private fun calculateShares(
        amount: BigDecimal,
        splitMethod: String,
        paidByUserId: Long,
        participants: List<ParticipantRequest>
    ): Map<Long, ShareCalc> = when (splitMethod) {
        SplitMethod.EQUAL -> calculateEqual(amount, paidByUserId, participants)
        SplitMethod.SHARES -> calculateByShares(amount, paidByUserId, participants)
        SplitMethod.EXACT -> calculateExact(amount, participants)
        else -> throw InvalidExpenseException("Unknown split method: $splitMethod")
    }

    private fun calculateEqual(amount: BigDecimal, paidByUserId: Long, participants: List<ParticipantRequest>): Map<Long, ShareCalc> {
        val n = participants.size
        val totalCents = amount.multiply(BigDecimal("100")).toLong()
        val baseCents = totalCents / n
        val remainderCents = totalCents % n
        val remainderTarget = participants.indexOfFirst { it.userId == paidByUserId }.takeIf { it >= 0 } ?: 0

        return participants.mapIndexed { i, p ->
            val cents = baseCents + if (i == remainderTarget) remainderCents else 0
            p.userId to ShareCalc(BigDecimal(cents).divide(BigDecimal("100")).setScale(2))
        }.toMap()
    }

    private fun calculateByShares(amount: BigDecimal, paidByUserId: Long, participants: List<ParticipantRequest>): Map<Long, ShareCalc> {
        val weights = participants.map {
            it.weight ?: throw InvalidExpenseException("Weight required for SHARES split method")
        }
        val totalWeight = weights.reduce(BigDecimal::add)
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0)
            throw InvalidExpenseException("Total weight cannot be zero")

        val rawShares = participants.zip(weights).map { (p, w) ->
            p.userId to amount.multiply(w).divide(totalWeight, 2, RoundingMode.HALF_UP)
        }

        val sumShares = rawShares.fold(BigDecimal.ZERO) { acc, (_, s) -> acc.add(s) }
        val remainder = amount.subtract(sumShares)
        val remainderTarget = participants.indexOfFirst { it.userId == paidByUserId }.takeIf { it >= 0 } ?: 0

        return rawShares.mapIndexed { i, (userId, share) ->
            userId to ShareCalc(
                share = if (i == remainderTarget) share.add(remainder) else share,
                weight = participants[i].weight
            )
        }.toMap()
    }

    private fun calculateExact(amount: BigDecimal, participants: List<ParticipantRequest>): Map<Long, ShareCalc> {
        val exactAmounts = participants.map {
            it.exactAmount ?: throw InvalidExpenseException("Exact amount required for EXACT split method")
        }
        val sum = exactAmounts.reduce(BigDecimal::add)
        if (sum.compareTo(amount) != 0)
            throw InvalidExpenseException("Sum of exact amounts ($sum) does not match expense amount ($amount)")

        return participants.zip(exactAmounts).associate { (p, exactAmount) ->
            p.userId to ShareCalc(share = exactAmount)
        }
    }
}
