package com.splittogether.backend.balance.service

import com.splittogether.backend.balance.dto.BalanceEntryResponse
import com.splittogether.backend.balance.dto.SimplifiedDebtResponse
import com.splittogether.backend.user.dto.UserBalanceResponse
import com.splittogether.backend.balance.entity.Balance
import com.splittogether.backend.balance.repository.BalanceRepository
import com.splittogether.backend.common.entity.Currency
import com.splittogether.backend.common.exception.CurrencyNotFoundException
import com.splittogether.backend.common.exception.GroupNotFoundException
import com.splittogether.backend.common.exception.NotGroupMemberException
import com.splittogether.backend.common.repository.CurrencyRepository
import com.splittogether.backend.currency.service.ExchangeRateService
import com.splittogether.backend.expense.repository.ExpenseParticipantRepository
import com.splittogether.backend.expense.repository.ExpenseRepository
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.group.service.MembershipGuard
import com.splittogether.backend.settlement.entity.SettlementStatus
import com.splittogether.backend.settlement.repository.SettlementRepository
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class BalanceService(
    private val balanceRepository: BalanceRepository,
    private val groupRepository: GroupRepository,
    private val membershipGuard: MembershipGuard,
    private val userRepository: UserRepository,
    private val expenseRepository: ExpenseRepository,
    private val expenseParticipantRepository: ExpenseParticipantRepository,
    private val settlementRepository: SettlementRepository,
    private val exchangeRateService: ExchangeRateService,
    private val currencyRepository: CurrencyRepository
) {

    private fun resolveCurrency(code: String): Currency =
        currencyRepository.findByCode(code) ?: throw CurrencyNotFoundException("Currency not found: $code")

    @Transactional
    fun updateBalance(groupId: Long, debtorId: Long, creditorId: Long, delta: BigDecimal) {
        if (delta.compareTo(BigDecimal.ZERO) == 0 || debtorId == creditorId) return

        val forward = balanceRepository.findByGroupIdAndDebtorIdAndCreditorId(groupId, debtorId, creditorId)
        val reverse = balanceRepository.findByGroupIdAndDebtorIdAndCreditorId(groupId, creditorId, debtorId)

        val currentNet = (forward?.amount ?: BigDecimal.ZERO)
            .subtract(reverse?.amount ?: BigDecimal.ZERO)
        val newNet = currentNet.add(delta)

        // Use JPQL bulk deletes so SQL DELETE runs before INSERT within the same transaction
        if (forward != null) balanceRepository.deleteByGroupIdAndDebtorIdAndCreditorId(groupId, debtorId, creditorId)
        if (reverse != null) balanceRepository.deleteByGroupIdAndDebtorIdAndCreditorId(groupId, creditorId, debtorId)

        val group = groupRepository.getReferenceById(groupId)
        when {
            newNet.compareTo(BigDecimal.ZERO) > 0 -> balanceRepository.save(
                Balance(
                    group = group,
                    debtor = userRepository.getReferenceById(debtorId),
                    creditor = userRepository.getReferenceById(creditorId),
                    amount = newNet
                )
            )
            newNet.compareTo(BigDecimal.ZERO) < 0 -> balanceRepository.save(
                Balance(
                    group = group,
                    debtor = userRepository.getReferenceById(creditorId),
                    creditor = userRepository.getReferenceById(debtorId),
                    amount = newNet.negate()
                )
            )
        }
    }

    @Transactional
    fun rebuildGroupBalances(groupId: Long) {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        val baseCurrency = group.baseCurrency

        val net = HashMap<Pair<Long, Long>, BigDecimal>()
        fun addDebt(debtorId: Long, creditorId: Long, amount: BigDecimal) {
            if (amount.signum() == 0 || debtorId == creditorId) return
            val (key, signed) = if (debtorId < creditorId) (debtorId to creditorId) to amount
                                else (creditorId to debtorId) to amount.negate()
            net[key] = (net[key] ?: BigDecimal.ZERO).add(signed)
        }

        val expenses = expenseRepository.findActiveByGroupId(groupId)
        if (expenses.isNotEmpty()) {
            val participantsByExpense = expenseParticipantRepository
                .findByExpenseIdIn(expenses.map { it.id })
                .groupBy { it.expense.id }
            for (expense in expenses) {
                val paidById = expense.paidBy.id
                for (p in participantsByExpense[expense.id] ?: emptyList()) {
                    val baseShare = exchangeRateService.convert(p.share, expense.currency, baseCurrency, expense.expenseDate)
                    p.baseShare = baseShare
                    expenseParticipantRepository.save(p)
                    if (p.user.id != paidById) addDebt(p.user.id, paidById, baseShare)
                }
            }
        }

        settlementRepository.findByGroupIdAndStatusCode(groupId, SettlementStatus.CONFIRMED).forEach { s ->
            val baseAmount = exchangeRateService.convert(s.amount, s.currency, baseCurrency, s.settlementDate)
            s.baseAmount = baseAmount
            settlementRepository.save(s)
            addDebt(s.payer.id, s.receiver.id, baseAmount.negate())
        }

        balanceRepository.deleteByGroupId(groupId)
        val groupRef = groupRepository.getReferenceById(groupId)
        val rows = net.mapNotNull { (pair, value) ->
            when (value.signum()) {
                0 -> null
                1 -> Balance(
                    group = groupRef,
                    debtor = userRepository.getReferenceById(pair.first),
                    creditor = userRepository.getReferenceById(pair.second),
                    amount = value
                )
                else -> Balance(
                    group = groupRef,
                    debtor = userRepository.getReferenceById(pair.second),
                    creditor = userRepository.getReferenceById(pair.first),
                    amount = value.negate()
                )
            }
        }
        balanceRepository.saveAll(rows)
    }

    @Transactional(readOnly = true)
    fun getBalances(userId: Long, groupId: Long, displayCurrencyCode: String? = null): List<BalanceEntryResponse> {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        membershipGuard.requireActiveMember(groupId, userId)

        val base = group.baseCurrency
        val target = displayCurrencyCode?.let { resolveCurrency(it) } ?: base
        val today = LocalDate.now()

        return balanceRepository.findByGroupId(groupId).map { b ->
            BalanceEntryResponse(
                debtorId = b.debtor.id,
                debtorName = b.debtor.displayName,
                creditorId = b.creditor.id,
                creditorName = b.creditor.displayName,
                amount = exchangeRateService.convertForDisplay(b.amount, base, target, today),
                currencyCode = target.code
            )
        }
    }

    @Transactional(readOnly = true)
    fun getSimplifiedDebts(userId: Long, groupId: Long, displayCurrencyCode: String? = null): List<SimplifiedDebtResponse> {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        membershipGuard.requireActiveMember(groupId, userId)

        val base = group.baseCurrency
        val target = displayCurrencyCode?.let { resolveCurrency(it) } ?: base
        val today = LocalDate.now()

        val balances = balanceRepository.findByGroupId(groupId)
        val userNames = balances.flatMap { listOf(it.debtor, it.creditor) }
            .associate { it.id to it.displayName }

        return computeSimplified(balances).map { (debtorId, creditorId, amount) ->
            SimplifiedDebtResponse(
                fromUserId = debtorId,
                fromName = userNames[debtorId] ?: "",
                toUserId = creditorId,
                toName = userNames[creditorId] ?: "",
                amount = exchangeRateService.convertForDisplay(amount, base, target, today),
                currencyCode = target.code
            )
        }
    }

    @Transactional
    fun simplifyBalances(userId: Long, groupId: Long, displayCurrencyCode: String? = null): List<BalanceEntryResponse> {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        val member = membershipGuard.requireActiveMember(groupId, userId)
        membershipGuard.requireAdminOrOwner(member)

        val base = group.baseCurrency
        val target = displayCurrencyCode?.let { resolveCurrency(it) } ?: base
        val today = LocalDate.now()

        val balances = balanceRepository.findByGroupId(groupId)
        if (balances.isEmpty()) return emptyList()

        val userNames = balances.flatMap { listOf(it.debtor, it.creditor) }
            .associate { it.id to it.displayName }

        val simplified = computeSimplified(balances)

        balanceRepository.deleteByGroupId(groupId)

        val groupRef = groupRepository.getReferenceById(groupId)
        balanceRepository.saveAll(
            simplified.map { (debtorId, creditorId, amount) ->
                Balance(
                    group = groupRef,
                    debtor = userRepository.getReferenceById(debtorId),
                    creditor = userRepository.getReferenceById(creditorId),
                    amount = amount
                )
            }
        )

        return simplified.map { (debtorId, creditorId, amount) ->
            BalanceEntryResponse(
                debtorId = debtorId,
                debtorName = userNames[debtorId] ?: "",
                creditorId = creditorId,
                creditorName = userNames[creditorId] ?: "",
                amount = exchangeRateService.convertForDisplay(amount, base, target, today),
                currencyCode = target.code
            )
        }
    }

    @Transactional(readOnly = true)
    fun getUserBalance(userId: Long, displayCurrencyCode: String? = null): UserBalanceResponse {
        val target = resolveCurrency(displayCurrencyCode ?: ExchangeRateService.RUB)
        val today = LocalDate.now()

        var totalOwed = BigDecimal.ZERO
        var totalOwing = BigDecimal.ZERO
        for (b in balanceRepository.findInvolvingUser(userId)) {
            val converted = exchangeRateService.convertForDisplay(b.amount, b.group.baseCurrency, target, today)
            if (b.creditor.id == userId) totalOwed = totalOwed.add(converted)
            if (b.debtor.id == userId) totalOwing = totalOwing.add(converted)
        }
        return UserBalanceResponse(
            totalOwed = totalOwed.setScale(2, RoundingMode.HALF_UP),
            totalOwing = totalOwing.setScale(2, RoundingMode.HALF_UP),
            netBalance = totalOwed.subtract(totalOwing).setScale(2, RoundingMode.HALF_UP),
            currencyCode = target.code
        )
    }

    private fun computeSimplified(balances: List<Balance>): List<Triple<Long, Long, BigDecimal>> {
        val net = mutableMapOf<Long, BigDecimal>()
        for (b in balances) {
            net[b.creditor.id] = (net[b.creditor.id] ?: BigDecimal.ZERO).add(b.amount)
            net[b.debtor.id] = (net[b.debtor.id] ?: BigDecimal.ZERO).subtract(b.amount)
        }

        val result = mutableListOf<Triple<Long, Long, BigDecimal>>()
        val remaining = net.filter { it.value.compareTo(BigDecimal.ZERO) != 0 }.toMutableMap()

        while (remaining.isNotEmpty()) {
            val creditorEntry = remaining.maxByOrNull { it.value } ?: break
            val debtorEntry = remaining.minByOrNull { it.value } ?: break

            if (creditorEntry.value <= BigDecimal.ZERO) break
            if (debtorEntry.value >= BigDecimal.ZERO) break

            val amount = creditorEntry.value.min(debtorEntry.value.negate())
            result.add(Triple(debtorEntry.key, creditorEntry.key, amount))

            val newCreditor = creditorEntry.value.subtract(amount)
            val newDebtor = debtorEntry.value.add(amount)

            if (newCreditor.compareTo(BigDecimal.ZERO) == 0) remaining.remove(creditorEntry.key)
            else remaining[creditorEntry.key] = newCreditor

            if (newDebtor.compareTo(BigDecimal.ZERO) == 0) remaining.remove(debtorEntry.key)
            else remaining[debtorEntry.key] = newDebtor
        }

        return result
    }

    @Transactional(readOnly = true)
    fun getNetBalance(userId: Long, groupId: Long): BigDecimal {
        val owed = balanceRepository.sumOwedInGroup(userId, groupId) ?: BigDecimal.ZERO
        val owing = balanceRepository.sumOwingInGroup(userId, groupId) ?: BigDecimal.ZERO
        return owed - owing
    }

    @Transactional(readOnly = true)
    fun getNetBalancesForUserInGroups(userId: Long, groupIds: List<Long>): Map<Long, BigDecimal> {
        if (groupIds.isEmpty()) return emptyMap()
        val owed = balanceRepository.sumOwedInGroups(userId, groupIds)
            .associate { it.groupId to (it.total ?: BigDecimal.ZERO) }
        val owing = balanceRepository.sumOwingInGroups(userId, groupIds)
            .associate { it.groupId to (it.total ?: BigDecimal.ZERO) }
        return (owed.keys + owing.keys).associateWith { groupId ->
            (owed[groupId] ?: BigDecimal.ZERO).subtract(owing[groupId] ?: BigDecimal.ZERO)
        }
    }
}
