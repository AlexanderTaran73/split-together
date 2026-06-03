package com.splittogether.backend.balance.service

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.RegisterRequest
import com.splittogether.backend.auth.service.AuthService
import com.splittogether.backend.common.exception.NotGroupMemberException
import com.splittogether.backend.expense.dto.CreateExpenseRequest
import com.splittogether.backend.expense.dto.ParticipantRequest
import com.splittogether.backend.expense.service.ExpenseService
import com.splittogether.backend.group.dto.CreateGroupRequest
import com.splittogether.backend.group.dto.CreateInvitationRequest
import com.splittogether.backend.group.dto.JoinGroupRequest
import com.splittogether.backend.group.repository.GroupInvitationRepository
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.group.service.GroupService
import com.splittogether.backend.user.entity.User
import com.splittogether.backend.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.*

class BalanceServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var balanceService: BalanceService
    @Autowired private lateinit var expenseService: ExpenseService
    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var groupRepository: GroupRepository
    @Autowired private lateinit var groupInvitationRepository: GroupInvitationRepository

    private fun createUser(email: String): User {
        authService.register(RegisterRequest(email, "Password1!", "User"))
        return userRepository.findByEmail(email)!!
    }

    private fun createGroup(ownerId: Long): com.splittogether.backend.group.entity.Group {
        groupService.createGroup(ownerId, CreateGroupRequest("Test Group", null, "RUB"))
        return groupRepository.findAll().last()
    }

    private fun joinGroup(userId: Long, ownerId: Long, groupId: Long) {
        val result = groupService.createInvitation(ownerId, groupId, CreateInvitationRequest("LINK"))
        groupService.joinGroup(userId, JoinGroupRequest(result.inviteCode!!))
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

    // ── getBalances ───────────────────────────────────────────────────────────

    @Test
    fun `getBalances returns all debts in the group`() {
        val owner = createUser("owner@test.com")
        val member = createUser("member@test.com")
        val group = createGroup(owner.id)
        joinGroup(member.id, owner.id, group.id)
        createExpense(owner.id, group.id, BigDecimal("30.00"), listOf(owner.id, member.id))

        val balances = balanceService.getBalances(owner.id, group.id)

        assertEquals(1, balances.size)
        assertEquals(member.id, balances[0].debtorId)
        assertEquals(owner.id, balances[0].creditorId)
        assertEquals(BigDecimal("15.00"), balances[0].amount)
    }

    @Test
    fun `getBalances returns empty list when no expenses`() {
        val owner = createUser("owner@test.com")
        val group = createGroup(owner.id)

        val balances = balanceService.getBalances(owner.id, group.id)

        assertTrue(balances.isEmpty())
    }

    @Test
    fun `getBalances throws NotGroupMemberException for non-member`() {
        val owner = createUser("owner@test.com")
        val outsider = createUser("outsider@test.com")
        val group = createGroup(owner.id)

        assertFailsWith<NotGroupMemberException> {
            balanceService.getBalances(outsider.id, group.id)
        }
    }

    // ── getSimplifiedDebts ────────────────────────────────────────────────────

    @Test
    fun `getSimplifiedDebts returns minimal transactions`() {
        val owner = createUser("owner@test.com")
        val m1 = createUser("m1@test.com")
        val m2 = createUser("m2@test.com")
        val group = createGroup(owner.id)
        joinGroup(m1.id, owner.id, group.id)
        joinGroup(m2.id, owner.id, group.id)

        // owner pays 30 split equally: m1 and m2 each owe owner 10
        createExpense(owner.id, group.id, BigDecimal("30.00"), listOf(owner.id, m1.id, m2.id))

        val simplified = balanceService.getSimplifiedDebts(owner.id, group.id)

        // Should produce at most 2 transactions (N-1 max)
        assertTrue(simplified.size <= 2)
        val total = simplified.fold(BigDecimal.ZERO) { acc, d -> acc.add(d.amount) }
        assertEquals(BigDecimal("20.00"), total)
    }

    @Test
    fun `getSimplifiedDebts cancels out mutual debts`() {
        val owner = createUser("owner@test.com")
        val member = createUser("member@test.com")
        val group = createGroup(owner.id)
        joinGroup(member.id, owner.id, group.id)

        // owner pays 30, member owes owner 15
        createExpense(owner.id, group.id, BigDecimal("30.00"), listOf(owner.id, member.id))
        // member pays 10, owner owes member 5 → net member owes owner 10
        createExpense(member.id, group.id, BigDecimal("10.00"), listOf(owner.id, member.id))

        val simplified = balanceService.getSimplifiedDebts(owner.id, group.id)

        assertEquals(1, simplified.size)
        assertEquals(member.id, simplified[0].fromUserId)
        assertEquals(owner.id, simplified[0].toUserId)
        assertEquals(BigDecimal("10.00"), simplified[0].amount)
    }

    @Test
    fun `getSimplifiedDebts returns empty when balances are zero`() {
        val owner = createUser("owner@test.com")
        val group = createGroup(owner.id)

        val simplified = balanceService.getSimplifiedDebts(owner.id, group.id)

        assertTrue(simplified.isEmpty())
    }

    // ── simplifyBalances ──────────────────────────────────────────────────────

    @Test
    fun `simplifyBalances persists simplified debts`() {
        val a = createUser("a@test.com")
        val b = createUser("b@test.com")
        val c = createUser("c@test.com")
        val group = createGroup(a.id)
        joinGroup(b.id, a.id, group.id)
        joinGroup(c.id, a.id, group.id)

        // A pays 20 for A+B: B owes A 10
        createExpense(a.id, group.id, BigDecimal("20.00"), listOf(a.id, b.id))
        // B pays 20 for B+C: C owes B 10
        // Net: A +10, B 0, C -10 → simplified: C owes A 10
        createExpense(b.id, group.id, BigDecimal("20.00"), listOf(b.id, c.id))

        assertEquals(2, balanceService.getBalances(a.id, group.id).size)

        val result = balanceService.simplifyBalances(a.id, group.id)

        assertEquals(1, result.size)
        assertEquals(c.id, result[0].debtorId)
        assertEquals(a.id, result[0].creditorId)
        assertEquals(BigDecimal("10.00"), result[0].amount)

        val persisted = balanceService.getBalances(a.id, group.id)
        assertEquals(1, persisted.size)
        assertEquals(c.id, persisted[0].debtorId)
        assertEquals(a.id, persisted[0].creditorId)
        assertEquals(BigDecimal("10.00"), persisted[0].amount)
    }

    @Test
    fun `simplifyBalances on empty balances returns empty`() {
        val owner = createUser("owner@test.com")
        val group = createGroup(owner.id)

        val result = balanceService.simplifyBalances(owner.id, group.id)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `new expense after simplify is appended incrementally`() {
        val a = createUser("a@test.com")
        val b = createUser("b@test.com")
        val c = createUser("c@test.com")
        val group = createGroup(a.id)
        joinGroup(b.id, a.id, group.id)
        joinGroup(c.id, a.id, group.id)

        createExpense(a.id, group.id, BigDecimal("20.00"), listOf(a.id, b.id))
        createExpense(b.id, group.id, BigDecimal("20.00"), listOf(b.id, c.id))
        balanceService.simplifyBalances(a.id, group.id)
        // Now: C→A 10

        // A pays 10 for A+C: C owes A 5 more → C→A 15
        createExpense(a.id, group.id, BigDecimal("10.00"), listOf(a.id, c.id))

        val balances = balanceService.getBalances(a.id, group.id)
        assertEquals(1, balances.size)
        assertEquals(c.id, balances[0].debtorId)
        assertEquals(a.id, balances[0].creditorId)
        assertEquals(BigDecimal("15.00"), balances[0].amount)
    }
}
