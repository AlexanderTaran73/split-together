package com.splittogether.backend.settlement.service

import com.splittogether.backend.balance.service.BalanceService
import com.splittogether.backend.common.exception.*
import com.splittogether.backend.common.repository.CurrencyRepository
import com.splittogether.backend.group.entity.GroupStatus
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.group.service.MembershipGuard
import com.splittogether.backend.settlement.dto.CreateSettlementRequest
import com.splittogether.backend.settlement.dto.SettlementResponse
import com.splittogether.backend.settlement.entity.Settlement
import com.splittogether.backend.settlement.entity.SettlementStatus
import com.splittogether.backend.settlement.repository.SettlementRepository
import com.splittogether.backend.settlement.repository.SettlementStatusRepository
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val settlementStatusRepository: SettlementStatusRepository,
    private val groupRepository: GroupRepository,
    private val membershipGuard: MembershipGuard,
    private val userRepository: UserRepository,
    private val currencyRepository: CurrencyRepository,
    private val balanceService: BalanceService
) {

    private fun requireActiveMember(groupId: Long, userId: Long) =
        membershipGuard.requireActiveMember(groupId, userId)

    private fun Settlement.toResponse() = SettlementResponse(
        id = id,
        groupId = group.id,
        payerId = payer.id,
        payerName = payer.displayName,
        receiverId = receiver.id,
        receiverName = receiver.displayName,
        amount = amount,
        currencyCode = currency.code,
        status = status.code,
        createdAt = createdAt,
        confirmedAt = confirmedAt,
        rejectedAt = rejectedAt
    )

    @Transactional
    fun createSettlement(userId: Long, groupId: Long, request: CreateSettlementRequest): SettlementResponse {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        if (group.status.code != GroupStatus.ACTIVE) throw GroupArchivedException("Group is archived")
        requireActiveMember(groupId, userId)

        if (userId == request.receiverId)
            throw InvalidSettlementException("Payer and receiver cannot be the same user")

        requireActiveMember(groupId, request.receiverId)

        val currency = currencyRepository.findByCode(request.currencyCode)
            ?: throw CurrencyNotFoundException("Currency not found: ${request.currencyCode}")
        val status = settlementStatusRepository.findByCode(SettlementStatus.PENDING)!!

        val payer = userRepository.getReferenceById(userId)
        val receiver = userRepository.getReferenceById(request.receiverId)

        val settlement = settlementRepository.save(
            Settlement(
                group = group,
                payer = payer,
                receiver = receiver,
                amount = request.amount,
                currency = currency,
                status = status,
                createdBy = payer
            )
        )

        return settlement.toResponse()
    }

    @Transactional(readOnly = true)
    fun getSettlements(userId: Long, groupId: Long): List<SettlementResponse> {
        if (!groupRepository.existsById(groupId)) throw GroupNotFoundException("Group not found")
        requireActiveMember(groupId, userId)
        return settlementRepository.findByGroupIdOrderByCreatedAtDesc(groupId).map { it.toResponse() }
    }

    @Transactional
    fun confirmSettlement(userId: Long, groupId: Long, settlementId: Long): SettlementResponse {
        val settlement = getValidatedSettlement(userId, groupId, settlementId, requireReceiver = true)

        settlement.status = settlementStatusRepository.findByCode(SettlementStatus.CONFIRMED)!!
        settlement.confirmedAt = Instant.now()
        settlementRepository.save(settlement)

        balanceService.updateBalance(groupId, settlement.payer.id, settlement.receiver.id, settlement.amount.negate())

        return settlement.toResponse()
    }

    @Transactional
    fun rejectSettlement(userId: Long, groupId: Long, settlementId: Long): SettlementResponse {
        val settlement = getValidatedSettlement(userId, groupId, settlementId, requireReceiver = true)

        settlement.status = settlementStatusRepository.findByCode(SettlementStatus.REJECTED)!!
        settlement.rejectedAt = Instant.now()
        settlementRepository.save(settlement)

        return settlement.toResponse()
    }

    private fun getValidatedSettlement(
        userId: Long,
        groupId: Long,
        settlementId: Long,
        requireReceiver: Boolean
    ): Settlement {
        if (!groupRepository.existsById(groupId)) throw GroupNotFoundException("Group not found")
        requireActiveMember(groupId, userId)

        val settlement = settlementRepository.findById(settlementId)
            .orElseThrow { SettlementNotFoundException("Settlement not found") }

        if (settlement.group.id != groupId)
            throw SettlementNotFoundException("Settlement not found")

        if (requireReceiver && settlement.receiver.id != userId)
            throw InsufficientPermissionsException("Only the receiver can perform this action")

        if (settlement.status.code != SettlementStatus.PENDING)
            throw InvalidSettlementException("Settlement is already ${settlement.status.code.lowercase()}")

        return settlement
    }
}
