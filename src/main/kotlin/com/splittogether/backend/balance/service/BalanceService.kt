package com.splittogether.backend.balance.service

import com.splittogether.backend.balance.dto.BalanceEntryResponse
import com.splittogether.backend.balance.dto.SimplifiedDebtResponse
import com.splittogether.backend.balance.entity.Balance
import com.splittogether.backend.balance.repository.BalanceRepository
import com.splittogether.backend.common.exception.GroupNotFoundException
import com.splittogether.backend.common.exception.NotGroupMemberException
import com.splittogether.backend.group.entity.MembershipStatus
import com.splittogether.backend.group.repository.GroupMemberRepository
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class BalanceService(
    private val balanceRepository: BalanceRepository,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val userRepository: UserRepository
) {

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

    @Transactional(readOnly = true)
    fun getBalances(userId: Long, groupId: Long): List<BalanceEntryResponse> {
        if (!groupRepository.existsById(groupId)) throw GroupNotFoundException("Group not found")
        groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            ?.takeIf { it.status.code == MembershipStatus.ACTIVE }
            ?: throw NotGroupMemberException("You are not a member of this group")

        return balanceRepository.findByGroupId(groupId).map { b ->
            BalanceEntryResponse(
                debtorId = b.debtor.id,
                debtorName = b.debtor.displayName,
                creditorId = b.creditor.id,
                creditorName = b.creditor.displayName,
                amount = b.amount
            )
        }
    }

    @Transactional(readOnly = true)
    fun getSimplifiedDebts(userId: Long, groupId: Long): List<SimplifiedDebtResponse> {
        if (!groupRepository.existsById(groupId)) throw GroupNotFoundException("Group not found")
        groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            ?.takeIf { it.status.code == MembershipStatus.ACTIVE }
            ?: throw NotGroupMemberException("You are not a member of this group")

        val balances = balanceRepository.findByGroupId(groupId)
        val userNames = balances.flatMap { listOf(it.debtor, it.creditor) }
            .associate { it.id to it.displayName }

        return computeSimplified(balances).map { (debtorId, creditorId, amount) ->
            SimplifiedDebtResponse(
                fromUserId = debtorId,
                fromName = userNames[debtorId] ?: "",
                toUserId = creditorId,
                toName = userNames[creditorId] ?: "",
                amount = amount
            )
        }
    }

    @Transactional
    fun simplifyBalances(userId: Long, groupId: Long): List<BalanceEntryResponse> {
        if (!groupRepository.existsById(groupId)) throw GroupNotFoundException("Group not found")
        groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            ?.takeIf { it.status.code == MembershipStatus.ACTIVE }
            ?: throw NotGroupMemberException("You are not a member of this group")

        val balances = balanceRepository.findByGroupId(groupId)
        if (balances.isEmpty()) return emptyList()

        val userNames = balances.flatMap { listOf(it.debtor, it.creditor) }
            .associate { it.id to it.displayName }

        val simplified = computeSimplified(balances)

        balanceRepository.deleteByGroupId(groupId)

        val group = groupRepository.getReferenceById(groupId)
        balanceRepository.saveAll(
            simplified.map { (debtorId, creditorId, amount) ->
                Balance(
                    group = group,
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
                amount = amount
            )
        }
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
}
