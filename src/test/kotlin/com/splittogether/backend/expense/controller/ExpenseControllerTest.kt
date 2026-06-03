package com.splittogether.backend.expense.controller

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.LoginRequest
import com.splittogether.backend.auth.dto.RegisterRequest
import com.splittogether.backend.auth.dto.VerifyEmailRequest
import com.splittogether.backend.auth.repository.EmailVerificationRepository
import com.splittogether.backend.auth.service.AuthService
import com.splittogether.backend.expense.dto.CreateExpenseRequest
import com.splittogether.backend.expense.dto.ParticipantRequest
import com.splittogether.backend.expense.service.ExpenseService
import com.splittogether.backend.group.dto.CreateGroupRequest
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.group.service.GroupService
import com.splittogether.backend.user.entity.User
import com.splittogether.backend.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDate

class ExpenseControllerTest : AbstractIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var expenseService: ExpenseService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var groupRepository: GroupRepository
    @Autowired private lateinit var emailVerificationRepository: EmailVerificationRepository

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun registerAndVerify(email: String = "owner@test.com"): User {
        authService.register(RegisterRequest(email, "Password1!", "User"))
        val user = userRepository.findByEmail(email)!!
        val code = emailVerificationRepository.findLatestUnused(user, "REGISTRATION")!!.code
        authService.verifyEmail(VerifyEmailRequest(email, code))
        return user
    }

    private fun token(email: String = "owner@test.com") =
        authService.login(LoginRequest(email, "Password1!")).accessToken

    private fun createGroup(userId: Long): com.splittogether.backend.group.entity.Group {
        groupService.createGroup(userId, CreateGroupRequest("Test Group", null, "RUB"))
        return groupRepository.findAll().last()
    }

    private fun createExpense(userId: Long, groupId: Long): Long {
        val response = expenseService.createExpense(
            userId, groupId,
            CreateExpenseRequest(
                title = "Dinner",
                amount = BigDecimal("30.00"),
                currencyCode = "RUB",
                splitMethod = "EQUAL",
                expenseDate = LocalDate.now(),
                paidByUserId = userId,
                participants = listOf(ParticipantRequest(userId))
            )
        )
        return response.id
    }

    // ── POST /expenses ────────────────────────────────────────────────────────

    @Test
    fun `POST expenses returns 201 for valid request`() {
        val user = registerAndVerify()
        val group = createGroup(user.id)
        val token = token()

        mockMvc.perform(
            post("/api/v1/groups/${group.id}/expenses")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title":"Dinner",
                        "amount":30.00,
                        "currencyCode":"RUB",
                        "splitMethod":"EQUAL",
                        "expenseDate":"${LocalDate.now()}",
                        "paidByUserId":${user.id},
                        "participants":[{"userId":${user.id}}]
                    }
                """.trimIndent())
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Dinner"))
            .andExpect(jsonPath("$.amount").value(30.0))
    }

    @Test
    fun `POST expenses returns 400 for blank title`() {
        val user = registerAndVerify()
        val group = createGroup(user.id)
        val token = token()

        mockMvc.perform(
            post("/api/v1/groups/${group.id}/expenses")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"","amount":30.00,"currencyCode":"RUB","splitMethod":"EQUAL","expenseDate":"${LocalDate.now()}","paidByUserId":${user.id},"participants":[{"userId":${user.id}}]}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST expenses returns 401 without token`() {
        mockMvc.perform(
            post("/api/v1/groups/1/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isUnauthorized)
    }

    // ── GET /expenses ─────────────────────────────────────────────────────────

    @Test
    fun `GET expenses returns 200 with expense list`() {
        val user = registerAndVerify()
        val group = createGroup(user.id)
        val token = token()
        createExpense(user.id, group.id)

        mockMvc.perform(
            get("/api/v1/groups/${group.id}/expenses")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title").value("Dinner"))
    }

    @Test
    fun `GET expenses returns 403 for non-member`() {
        val owner = registerAndVerify()
        val other = registerAndVerify("other@test.com")
        val otherToken = token("other@test.com")
        val group = createGroup(owner.id)

        mockMvc.perform(
            get("/api/v1/groups/${group.id}/expenses")
                .header("Authorization", "Bearer $otherToken")
        ).andExpect(status().isForbidden)
    }

    // ── GET /expenses/{id} ────────────────────────────────────────────────────

    @Test
    fun `GET expense by id returns 200`() {
        val user = registerAndVerify()
        val group = createGroup(user.id)
        val token = token()
        val expenseId = createExpense(user.id, group.id)

        mockMvc.perform(
            get("/api/v1/groups/${group.id}/expenses/$expenseId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(expenseId))
    }

    @Test
    fun `GET expense returns 404 for unknown id`() {
        val user = registerAndVerify()
        val group = createGroup(user.id)
        val token = token()

        mockMvc.perform(
            get("/api/v1/groups/${group.id}/expenses/999")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNotFound)
    }

    // ── PUT /expenses/{id} ────────────────────────────────────────────────────

    @Test
    fun `PUT expense returns 200 with updated data`() {
        val user = registerAndVerify()
        val group = createGroup(user.id)
        val token = token()
        val expenseId = createExpense(user.id, group.id)

        mockMvc.perform(
            put("/api/v1/groups/${group.id}/expenses/$expenseId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Updated","amount":50.00,"currencyCode":"RUB","splitMethod":"EQUAL","expenseDate":"${LocalDate.now()}","paidByUserId":${user.id},"participants":[{"userId":${user.id}}]}""")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Updated"))
            .andExpect(jsonPath("$.amount").value(50.0))
    }

    // ── DELETE /expenses/{id} ─────────────────────────────────────────────────

    @Test
    fun `DELETE expense returns 204`() {
        val user = registerAndVerify()
        val group = createGroup(user.id)
        val token = token()
        val expenseId = createExpense(user.id, group.id)

        mockMvc.perform(
            delete("/api/v1/groups/${group.id}/expenses/$expenseId")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE expense returns 401 without token`() {
        mockMvc.perform(delete("/api/v1/groups/1/expenses/1"))
            .andExpect(status().isUnauthorized)
    }
}
