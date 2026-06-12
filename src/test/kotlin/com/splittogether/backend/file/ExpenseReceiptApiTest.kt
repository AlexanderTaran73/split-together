package com.splittogether.backend.file

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.LoginRequest
import com.splittogether.backend.auth.service.AuthService
import com.splittogether.backend.expense.dto.CreateExpenseRequest
import com.splittogether.backend.expense.dto.ParticipantRequest
import com.splittogether.backend.expense.service.ExpenseService
import com.splittogether.backend.group.dto.CreateGroupRequest
import com.splittogether.backend.group.service.GroupService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

class ExpenseReceiptApiTest : AbstractIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var expenseService: ExpenseService

    private fun token(email: String): String =
        authService.login(LoginRequest(email, generator.defaultPassword)).accessToken

    private fun receipt() =
        MockMultipartFile("file", "receipt.jpg", "image/jpeg", byteArrayOf(9, 8, 7))

    @Test
    fun `upload list and delete a receipt`() {
        val owner = generator.user(email = "owner@test.com")
        val group = groupService.createGroup(owner.id, CreateGroupRequest("G", null, "RUB"))
        val expense = expenseService.createExpense(
            owner.id, group.id,
            CreateExpenseRequest(
                title = "Taxi",
                amount = BigDecimal("10.00"),
                currencyCode = "RUB",
                splitMethod = "EQUAL",
                expenseDate = LocalDate.now(),
                paidByUserId = owner.id,
                participants = listOf(ParticipantRequest(owner.id))
            )
        )
        val base = "/api/v1/groups/${group.id}/expenses/${expense.id}/receipts"

        mockMvc.perform(
            multipart(base).file(receipt())
                .header("Authorization", "Bearer ${token("owner@test.com")}")
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.originalName").value("receipt.jpg"))
            .andExpect(jsonPath("$.url").isNotEmpty)

        mockMvc.perform(
            get(base).header("Authorization", "Bearer ${token("owner@test.com")}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `receipt for an expense from another group returns 404`() {
        val owner = generator.user(email = "owner@test.com")
        val group1 = groupService.createGroup(owner.id, CreateGroupRequest("G1", null, "RUB"))
        val group2 = groupService.createGroup(owner.id, CreateGroupRequest("G2", null, "RUB"))
        val expense = expenseService.createExpense(
            owner.id, group1.id,
            CreateExpenseRequest(
                title = "Taxi",
                amount = BigDecimal("10.00"),
                currencyCode = "RUB",
                splitMethod = "EQUAL",
                expenseDate = LocalDate.now(),
                paidByUserId = owner.id,
                participants = listOf(ParticipantRequest(owner.id))
            )
        )

        mockMvc.perform(
            multipart("/api/v1/groups/${group2.id}/expenses/${expense.id}/receipts").file(receipt())
                .header("Authorization", "Bearer ${token("owner@test.com")}")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `upload receipt by non-member returns 403`() {
        val owner = generator.user(email = "owner@test.com")
        generator.user(email = "outsider@test.com")
        val group = groupService.createGroup(owner.id, CreateGroupRequest("G", null, "RUB"))
        val expense = expenseService.createExpense(
            owner.id, group.id,
            CreateExpenseRequest(
                title = "Taxi",
                amount = BigDecimal("10.00"),
                currencyCode = "RUB",
                splitMethod = "EQUAL",
                expenseDate = LocalDate.now(),
                paidByUserId = owner.id,
                participants = listOf(ParticipantRequest(owner.id))
            )
        )

        mockMvc.perform(
            multipart("/api/v1/groups/${group.id}/expenses/${expense.id}/receipts").file(receipt())
                .header("Authorization", "Bearer ${token("outsider@test.com")}")
        ).andExpect(status().isForbidden)
    }
}
