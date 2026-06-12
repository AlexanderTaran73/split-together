package com.splittogether.backend.user.service

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.common.exception.UserNotFoundException
import com.splittogether.backend.expense.dto.CreateExpenseRequest
import com.splittogether.backend.expense.dto.ParticipantRequest
import com.splittogether.backend.expense.service.ExpenseService
import com.splittogether.backend.group.dto.CreateGroupRequest
import com.splittogether.backend.group.dto.CreateInvitationRequest
import com.splittogether.backend.group.dto.JoinGroupRequest
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.group.service.GroupService
import com.splittogether.backend.user.dto.UpdateProfileRequest
import com.splittogether.backend.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UserServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var userService: UserService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var expenseService: ExpenseService
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

    // ── getMe ─────────────────────────────────────────────────────────────────

    @Test
    fun `getMe returns correct user response`() {
        val user = generator.user(email = "user@test.com", displayName = "Test User")

        val response = userService.getMe(user.id)

        assertEquals(user.id, response.id)
        assertEquals(user.email, response.email)
        assertEquals(user.displayName, response.displayName)
    }

    @Test
    fun `getMe throws UserNotFoundException for unknown id`() {
        assertFailsWith<UserNotFoundException> {
            userService.getMe(999L)
        }
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Test
    fun `search returns users matching displayName`() {
        generator.user(email = "alice@test.com", displayName = "Alice Smith")
        generator.user(email = "other@test.com", displayName = "Bob Jones")

        val results = userService.search("Alice")

        assertEquals(1, results.size)
        assertEquals("Alice Smith", results[0].displayName)
    }

    @Test
    fun `search returns users matching email`() {
        generator.user(email = "alice@test.com")
        generator.user(email = "bob@test.com")

        val results = userService.search("alice")

        assertEquals(1, results.size)
        assertEquals("alice@test.com", results[0].email)
    }

    @Test
    fun `search is case insensitive`() {
        generator.user(email = "alice@test.com", displayName = "Alice Smith")

        val results = userService.search("ALICE")

        assertEquals(1, results.size)
    }

    @Test
    fun `search returns empty list when no match`() {
        generator.user()

        val results = userService.search("xyz_no_match")

        assertTrue(results.isEmpty())
    }

    // ── updateMe ──────────────────────────────────────────────────────────────

    @Test
    fun `updateMe updates displayName and persists the change`() {
        val user = generator.user(email = "user@test.com", displayName = "Old Name")

        val response = userService.updateMe(user.id, UpdateProfileRequest("New Name"))

        assertEquals("New Name", response.displayName)
        assertEquals("New Name", userRepository.findById(user.id).get().displayName)
    }

    @Test
    fun `updateMe throws UserNotFoundException for unknown id`() {
        assertFailsWith<UserNotFoundException> {
            userService.updateMe(999L, UpdateProfileRequest("Name"))
        }
    }

    // ── getMyGroups ───────────────────────────────────────────────────────────

    @Test
    fun `getMyGroups returns active groups of the user`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        joinGroup(member.id, owner.id, group.id)

        val ownerGroups = userService.getMyGroups(owner.id)
        val memberGroups = userService.getMyGroups(member.id)

        assertEquals(1, ownerGroups.size)
        assertEquals(group.id, ownerGroups[0].id)
        assertEquals(1, memberGroups.size)
        assertEquals(group.id, memberGroups[0].id)
    }

    @Test
    fun `getMyGroups returns empty when user has no groups`() {
        val user = generator.user()

        val groups = userService.getMyGroups(user.id)

        assertTrue(groups.isEmpty())
    }

    // ── getMyBalance ──────────────────────────────────────────────────────────

    @Test
    fun `getMyBalance returns correct totals`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        joinGroup(member.id, owner.id, group.id)
        createExpense(owner.id, group.id, BigDecimal("30.00"), listOf(owner.id, member.id))

        val balance = userService.getMyBalance(owner.id)

        assertEquals(BigDecimal("15.00"), balance.totalOwed)
        assertEquals(BigDecimal("0.00"), balance.totalOwing)
        assertEquals(BigDecimal("15.00"), balance.netBalance)
    }

    @Test
    fun `getMyBalance returns zeros when no balances`() {
        val user = generator.user()

        val balance = userService.getMyBalance(user.id)

        assertEquals(BigDecimal("0.00"), balance.totalOwed)
        assertEquals(BigDecimal("0.00"), balance.totalOwing)
        assertEquals(BigDecimal("0.00"), balance.netBalance)
    }
}
