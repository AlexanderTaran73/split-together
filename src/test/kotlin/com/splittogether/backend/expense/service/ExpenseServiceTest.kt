package com.splittogether.backend.expense.service

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.balance.repository.BalanceRepository
import com.splittogether.backend.common.exception.*
import com.splittogether.backend.expense.dto.CreateExpenseRequest
import com.splittogether.backend.expense.dto.ParticipantRequest
import com.splittogether.backend.expense.dto.UpdateExpenseRequest
import com.splittogether.backend.expense.repository.ExpenseParticipantRepository
import com.splittogether.backend.expense.repository.ExpenseRepository
import com.splittogether.backend.group.dto.CreateGroupRequest
import com.splittogether.backend.group.dto.CreateInvitationRequest
import com.splittogether.backend.group.dto.JoinGroupRequest
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.group.service.GroupService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.*

class ExpenseServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var expenseService: ExpenseService
    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var groupRepository: GroupRepository
    @Autowired private lateinit var expenseRepository: ExpenseRepository
    @Autowired private lateinit var expenseParticipantRepository: ExpenseParticipantRepository
    @Autowired private lateinit var balanceRepository: BalanceRepository

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun createGroup(ownerId: Long): com.splittogether.backend.group.entity.Group {
        groupService.createGroup(ownerId, CreateGroupRequest("Test Group", null, "RUB"))
        return groupRepository.findAll().last()
    }

    private fun joinGroup(userId: Long, ownerId: Long, groupId: Long) {
        val result = groupService.createInvitation(ownerId, groupId, CreateInvitationRequest("LINK"))
        groupService.joinGroup(userId, JoinGroupRequest(result.inviteCode!!))
    }

    private fun equalExpenseRequest(paidByUserId: Long, participantIds: List<Long>, amount: BigDecimal = BigDecimal("30.00")) =
        CreateExpenseRequest(
            title = "Dinner",
            amount = amount,
            currencyCode = "RUB",
            splitMethod = "EQUAL",
            expenseDate = LocalDate.now(),
            paidByUserId = paidByUserId,
            participants = participantIds.map { ParticipantRequest(it) }
        )

    // ── createExpense — EQUAL ─────────────────────────────────────────────────

    @Test
    fun `createExpense EQUAL splits amount evenly among participants`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        joinGroup(member.id, owner.id, group.id)

        val response = expenseService.createExpense(
            owner.id, group.id,
            equalExpenseRequest(owner.id, listOf(owner.id, member.id), BigDecimal("30.00"))
        )

        assertEquals(2, response.participants.size)
        response.participants.forEach { assertEquals(BigDecimal("15.00"), it.share) }
    }

    @Test
    fun `createExpense EQUAL remainder goes to payer`() {
        val owner = generator.user(email = "owner@test.com")
        val m1 = generator.user(email = "m1@test.com")
        val m2 = generator.user(email = "m2@test.com")
        val group = createGroup(owner.id)
        joinGroup(m1.id, owner.id, group.id)
        joinGroup(m2.id, owner.id, group.id)

        val response = expenseService.createExpense(
            owner.id, group.id,
            equalExpenseRequest(owner.id, listOf(owner.id, m1.id, m2.id), BigDecimal("10.00"))
        )

        val payerShare = response.participants.first { it.userId == owner.id }.share
        val otherShares = response.participants.filter { it.userId != owner.id }.map { it.share }
        assertEquals(BigDecimal("3.34"), payerShare)
        otherShares.forEach { assertEquals(BigDecimal("3.33"), it) }
    }

    @Test
    fun `createExpense EQUAL updates balances correctly`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        joinGroup(member.id, owner.id, group.id)

        expenseService.createExpense(
            owner.id, group.id,
            equalExpenseRequest(owner.id, listOf(owner.id, member.id), BigDecimal("30.00"))
        )

        val balance = balanceRepository.findByGroupIdAndDebtorIdAndCreditorId(group.id, member.id, owner.id)
        assertNotNull(balance)
        assertEquals(BigDecimal("15.00"), balance!!.amount)
    }

    // ── createExpense — SHARES ────────────────────────────────────────────────

    @Test
    fun `createExpense SHARES distributes by weight`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        joinGroup(member.id, owner.id, group.id)

        val response = expenseService.createExpense(
            owner.id, group.id,
            CreateExpenseRequest(
                title = "Hotel",
                amount = BigDecimal("100.00"),
                currencyCode = "RUB",
                splitMethod = "SHARES",
                expenseDate = LocalDate.now(),
                paidByUserId = owner.id,
                participants = listOf(
                    ParticipantRequest(owner.id, weight = BigDecimal("3")),
                    ParticipantRequest(member.id, weight = BigDecimal("1"))
                )
            )
        )

        val ownerShare = response.participants.first { it.userId == owner.id }.share
        val memberShare = response.participants.first { it.userId == member.id }.share
        assertEquals(BigDecimal("75.00"), ownerShare)
        assertEquals(BigDecimal("25.00"), memberShare)
    }

    @Test
    fun `createExpense SHARES throws when weight is missing`() {
        val owner = generator.user(email = "owner@test.com")
        val group = createGroup(owner.id)

        assertFailsWith<InvalidExpenseException> {
            expenseService.createExpense(
                owner.id, group.id,
                CreateExpenseRequest(
                    title = "Test", amount = BigDecimal("10"), currencyCode = "RUB",
                    splitMethod = "SHARES", expenseDate = LocalDate.now(),
                    paidByUserId = owner.id,
                    participants = listOf(ParticipantRequest(owner.id))
                )
            )
        }
    }

    // ── createExpense — EXACT ─────────────────────────────────────────────────

    @Test
    fun `createExpense EXACT uses provided amounts`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        joinGroup(member.id, owner.id, group.id)

        val response = expenseService.createExpense(
            owner.id, group.id,
            CreateExpenseRequest(
                title = "Groceries",
                amount = BigDecimal("70.00"),
                currencyCode = "RUB",
                splitMethod = "EXACT",
                expenseDate = LocalDate.now(),
                paidByUserId = owner.id,
                participants = listOf(
                    ParticipantRequest(owner.id, exactAmount = BigDecimal("40.00")),
                    ParticipantRequest(member.id, exactAmount = BigDecimal("30.00"))
                )
            )
        )

        assertEquals(BigDecimal("40.00"), response.participants.first { it.userId == owner.id }.share)
        assertEquals(BigDecimal("30.00"), response.participants.first { it.userId == member.id }.share)
    }

    @Test
    fun `createExpense EXACT throws when sum does not match amount`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        joinGroup(member.id, owner.id, group.id)

        assertFailsWith<InvalidExpenseException> {
            expenseService.createExpense(
                owner.id, group.id,
                CreateExpenseRequest(
                    title = "Test", amount = BigDecimal("100.00"), currencyCode = "RUB",
                    splitMethod = "EXACT", expenseDate = LocalDate.now(),
                    paidByUserId = owner.id,
                    participants = listOf(
                        ParticipantRequest(owner.id, exactAmount = BigDecimal("40.00")),
                        ParticipantRequest(member.id, exactAmount = BigDecimal("30.00"))
                    )
                )
            )
        }
    }

    // ── validation ────────────────────────────────────────────────────────────

    @Test
    fun `createExpense throws NotGroupMemberException for non-member`() {
        val owner = generator.user(email = "owner@test.com")
        val outsider = generator.user(email = "outsider@test.com")
        val group = createGroup(owner.id)

        assertFailsWith<NotGroupMemberException> {
            expenseService.createExpense(outsider.id, group.id, equalExpenseRequest(outsider.id, listOf(outsider.id)))
        }
    }

    @Test
    fun `createExpense throws CurrencyNotFoundException for unknown currency`() {
        val owner = generator.user(email = "owner@test.com")
        val group = createGroup(owner.id)

        assertFailsWith<CurrencyNotFoundException> {
            expenseService.createExpense(
                owner.id, group.id,
                equalExpenseRequest(owner.id, listOf(owner.id)).copy(currencyCode = "XYZ")
            )
        }
    }

    // ── updateExpense ─────────────────────────────────────────────────────────

    @Test
    fun `updateExpense recalculates balances correctly`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        joinGroup(member.id, owner.id, group.id)

        val created = expenseService.createExpense(
            owner.id, group.id,
            equalExpenseRequest(owner.id, listOf(owner.id, member.id), BigDecimal("30.00"))
        )

        expenseService.updateExpense(
            owner.id, group.id, created.id,
            UpdateExpenseRequest(
                title = "Updated", amount = BigDecimal("40.00"), currencyCode = "RUB",
                splitMethod = "EQUAL", expenseDate = LocalDate.now(),
                paidByUserId = owner.id,
                participants = listOf(ParticipantRequest(owner.id), ParticipantRequest(member.id))
            )
        )

        val balance = balanceRepository.findByGroupIdAndDebtorIdAndCreditorId(group.id, member.id, owner.id)
        assertNotNull(balance)
        assertEquals(BigDecimal("20.00"), balance!!.amount)
    }

    // ── deleteExpense ─────────────────────────────────────────────────────────

    @Test
    fun `deleteExpense soft-deletes and reverses balances`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        joinGroup(member.id, owner.id, group.id)

        val created = expenseService.createExpense(
            owner.id, group.id,
            equalExpenseRequest(owner.id, listOf(owner.id, member.id), BigDecimal("30.00"))
        )

        expenseService.deleteExpense(owner.id, group.id, created.id)

        val expense = expenseRepository.findById(created.id).get()
        assertNotNull(expense.deletedAt)

        val balance = balanceRepository.findByGroupIdAndDebtorIdAndCreditorId(group.id, member.id, owner.id)
        assertNull(balance)
    }

    @Test
    fun `deleteExpense throws for non-creator member`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        joinGroup(member.id, owner.id, group.id)

        val created = expenseService.createExpense(
            owner.id, group.id,
            equalExpenseRequest(owner.id, listOf(owner.id, member.id))
        )

        assertFailsWith<InsufficientPermissionsException> {
            expenseService.deleteExpense(member.id, group.id, created.id)
        }
    }

    // ── balance netting ───────────────────────────────────────────────────────

    @Test
    fun `balances are netted when bidirectional debts exist`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        joinGroup(member.id, owner.id, group.id)

        expenseService.createExpense(
            owner.id, group.id,
            equalExpenseRequest(owner.id, listOf(owner.id, member.id), BigDecimal("30.00"))
        )

        expenseService.createExpense(
            member.id, group.id,
            equalExpenseRequest(member.id, listOf(owner.id, member.id), BigDecimal("10.00"))
        )

        val balance = balanceRepository.findByGroupIdAndDebtorIdAndCreditorId(group.id, member.id, owner.id)
        assertNotNull(balance)
        assertEquals(BigDecimal("10.00"), balance!!.amount)
        assertNull(balanceRepository.findByGroupIdAndDebtorIdAndCreditorId(group.id, owner.id, member.id))
    }
}
