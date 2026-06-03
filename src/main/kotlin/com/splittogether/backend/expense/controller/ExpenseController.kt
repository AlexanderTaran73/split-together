package com.splittogether.backend.expense.controller

import com.splittogether.backend.auth.security.AppUserDetails
import com.splittogether.backend.expense.dto.CreateExpenseRequest
import com.splittogether.backend.expense.dto.ExpenseResponse
import com.splittogether.backend.expense.dto.UpdateExpenseRequest
import com.splittogether.backend.expense.service.ExpenseService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/groups/{groupId}/expenses")
class ExpenseController(private val expenseService: ExpenseService) {

    @GetMapping
    fun getExpenses(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<List<ExpenseResponse>> =
        ResponseEntity.ok(expenseService.getExpenses(user.userId, groupId))

    @GetMapping("/{expenseId}")
    fun getExpense(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @PathVariable expenseId: Long
    ): ResponseEntity<ExpenseResponse> =
        ResponseEntity.ok(expenseService.getExpense(user.userId, groupId, expenseId))

    @PostMapping
    fun createExpense(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: CreateExpenseRequest
    ): ResponseEntity<ExpenseResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(expenseService.createExpense(user.userId, groupId, request))

    @PutMapping("/{expenseId}")
    fun updateExpense(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @PathVariable expenseId: Long,
        @Valid @RequestBody request: UpdateExpenseRequest
    ): ResponseEntity<ExpenseResponse> =
        ResponseEntity.ok(expenseService.updateExpense(user.userId, groupId, expenseId, request))

    @DeleteMapping("/{expenseId}")
    fun deleteExpense(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @PathVariable expenseId: Long
    ): ResponseEntity<Void> {
        expenseService.deleteExpense(user.userId, groupId, expenseId)
        return ResponseEntity.noContent().build()
    }
}
