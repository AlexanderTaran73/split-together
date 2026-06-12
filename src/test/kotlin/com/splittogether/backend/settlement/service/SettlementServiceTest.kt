package com.splittogether.backend.settlement.service

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.balance.service.BalanceService
import com.splittogether.backend.common.exception.InsufficientPermissionsException
import com.splittogether.backend.common.exception.InvalidSettlementException
import com.splittogether.backend.expense.dto.CreateExpenseRequest
import com.splittogether.backend.expense.dto.ParticipantRequest
import com.splittogether.backend.expense.service.ExpenseService
import com.splittogether.backend.group.dto.CreateGroupRequest
import com.splittogether.backend.group.dto.CreateInvitationRequest
import com.splittogether.backend.group.dto.JoinGroupRequest
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.group.service.GroupService
import com.splittogether.backend.settlement.dto.CreateSettlementRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.*

class SettlementServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var settlementService: SettlementService
    @Autowired private lateinit var balanceService: BalanceService
    @Autowired private lateinit var expenseService: ExpenseService
    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var groupRepository: GroupRepository

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun createGroup(ownerId: Long): com.splittogether.backend.group.entity.Group {
        groupService.createGroup(ownerId, CreateGroupRequest("Test Group", null, "RUB"))
        return groupRepository.findAll().last()
    }

    private fun joinGroup(userId: Long, ownerId: Long, groupId: Long) {
        val result = groupService.createInvitation(ownerId, groupId, CreateInvitationRequest("LINK"))
        groupService.joinGroup(userId, JoinGroupRequest(result.token!!))
    }

    private fun createExpense(payerId: Long, groupId: Long, amount: BigDecimal, participantIds: List<Long>) =
        expenseService.createExpense(
            payerId, groupId,
            CreateExpenseRequest(
                title = "Expense", amount = amount, currencyCode = "RUB",
                splitMethod = "EQUAL", expenseDate = LocalDate.now(),
                paidByUserId = payerId, participants = participantIds.map { ParticipantRequest(it) }
            )
        )

    private fun createSettlement(payerId: Long, groupId: Long, receiverId: Long, amount: BigDecimal) =
        settlementService.createSettlement(
            payerId, groupId,
            CreateSettlementRequest(receiverId = receiverId, amount = amount, currencyCode = "RUB")
        )

    // ── createSettlement ──────────────────────────────────────────────────────

    @Test
    fun `createSettlement creates pending settlement without affecting balances`() {
        val payer = generator.user(email = "payer@test.com")
        val receiver = generator.user(email = "receiver@test.com")
        val group = createGroup(payer.id)
        joinGroup(receiver.id, payer.id, group.id)
        createExpense(receiver.id, group.id, BigDecimal("20.00"), listOf(payer.id, receiver.id))

        val balanceBefore = balanceService.getBalances(payer.id, group.id)
        val result = createSettlement(payer.id, group.id, receiver.id, BigDecimal("10.00"))

        assertEquals("PENDING", result.status)
        assertEquals(payer.id, result.payerId)
        assertEquals(receiver.id, result.receiverId)
        assertEquals(BigDecimal("10.00"), result.amount)
        assertNull(result.confirmedAt)
        assertNull(result.rejectedAt)

        assertEquals(balanceBefore, balanceService.getBalances(payer.id, group.id))
    }

    @Test
    fun `createSettlement fails when payer equals receiver`() {
        val owner = generator.user(email = "owner@test.com")
        val group = createGroup(owner.id)

        assertFailsWith<InvalidSettlementException> {
            createSettlement(owner.id, group.id, owner.id, BigDecimal("10.00"))
        }
    }

    // ── confirmSettlement ─────────────────────────────────────────────────────

    @Test
    fun `confirmSettlement updates balance and sets confirmedAt`() {
        val payer = generator.user(email = "payer@test.com")
        val receiver = generator.user(email = "receiver@test.com")
        val group = createGroup(payer.id)
        joinGroup(receiver.id, payer.id, group.id)
        createExpense(receiver.id, group.id, BigDecimal("20.00"), listOf(payer.id, receiver.id))

        val settlement = createSettlement(payer.id, group.id, receiver.id, BigDecimal("10.00"))
        val result = settlementService.confirmSettlement(receiver.id, group.id, settlement.id)

        assertEquals("CONFIRMED", result.status)
        assertNotNull(result.confirmedAt)

        val balances = balanceService.getBalances(payer.id, group.id)
        assertTrue(balances.isEmpty())
    }

    @Test
    fun `confirmSettlement reduces debt partially when amount is less than balance`() {
        val payer = generator.user(email = "payer@test.com")
        val receiver = generator.user(email = "receiver@test.com")
        val group = createGroup(payer.id)
        joinGroup(receiver.id, payer.id, group.id)
        createExpense(receiver.id, group.id, BigDecimal("30.00"), listOf(payer.id, receiver.id))

        val settlement = createSettlement(payer.id, group.id, receiver.id, BigDecimal("10.00"))
        settlementService.confirmSettlement(receiver.id, group.id, settlement.id)

        val balances = balanceService.getBalances(payer.id, group.id)
        assertEquals(1, balances.size)
        assertEquals(payer.id, balances[0].debtorId)
        assertEquals(receiver.id, balances[0].creditorId)
        assertEquals(BigDecimal("5.00"), balances[0].amount)
    }

    @Test
    fun `confirmSettlement fails when called by non-receiver`() {
        val payer = generator.user(email = "payer@test.com")
        val receiver = generator.user(email = "receiver@test.com")
        val group = createGroup(payer.id)
        joinGroup(receiver.id, payer.id, group.id)

        val settlement = createSettlement(payer.id, group.id, receiver.id, BigDecimal("10.00"))

        assertFailsWith<InsufficientPermissionsException> {
            settlementService.confirmSettlement(payer.id, group.id, settlement.id)
        }
    }

    @Test
    fun `confirmSettlement fails when settlement is already confirmed`() {
        val payer = generator.user(email = "payer@test.com")
        val receiver = generator.user(email = "receiver@test.com")
        val group = createGroup(payer.id)
        joinGroup(receiver.id, payer.id, group.id)

        val settlement = createSettlement(payer.id, group.id, receiver.id, BigDecimal("10.00"))
        settlementService.confirmSettlement(receiver.id, group.id, settlement.id)

        assertFailsWith<InvalidSettlementException> {
            settlementService.confirmSettlement(receiver.id, group.id, settlement.id)
        }
    }

    // ── rejectSettlement ──────────────────────────────────────────────────────

    @Test
    fun `rejectSettlement sets status and does not affect balances`() {
        val payer = generator.user(email = "payer@test.com")
        val receiver = generator.user(email = "receiver@test.com")
        val group = createGroup(payer.id)
        joinGroup(receiver.id, payer.id, group.id)
        createExpense(receiver.id, group.id, BigDecimal("20.00"), listOf(payer.id, receiver.id))

        val settlement = createSettlement(payer.id, group.id, receiver.id, BigDecimal("10.00"))
        val balanceBefore = balanceService.getBalances(payer.id, group.id)

        val result = settlementService.rejectSettlement(receiver.id, group.id, settlement.id, null)

        assertEquals("REJECTED", result.status)
        assertNotNull(result.rejectedAt)
        assertEquals(balanceBefore, balanceService.getBalances(payer.id, group.id))
    }

    @Test
    fun `rejectSettlement fails when called by non-receiver`() {
        val payer = generator.user(email = "payer@test.com")
        val receiver = generator.user(email = "receiver@test.com")
        val group = createGroup(payer.id)
        joinGroup(receiver.id, payer.id, group.id)

        val settlement = createSettlement(payer.id, group.id, receiver.id, BigDecimal("10.00"))

        assertFailsWith<InsufficientPermissionsException> {
            settlementService.rejectSettlement(payer.id, group.id, settlement.id, null)
        }
    }

    // ── getSettlements ────────────────────────────────────────────────────────

    @Test
    fun `getSettlements returns all settlements ordered by createdAt desc`() {
        val payer = generator.user(email = "payer@test.com")
        val receiver = generator.user(email = "receiver@test.com")
        val group = createGroup(payer.id)
        joinGroup(receiver.id, payer.id, group.id)

        createSettlement(payer.id, group.id, receiver.id, BigDecimal("10.00"))
        createSettlement(payer.id, group.id, receiver.id, BigDecimal("20.00"))

        val settlements = settlementService.getSettlements(payer.id, group.id)

        assertEquals(2, settlements.size)
        assertEquals(BigDecimal("20.00"), settlements[0].amount)
        assertEquals(BigDecimal("10.00"), settlements[1].amount)
    }
}
