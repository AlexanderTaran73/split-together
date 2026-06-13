package com.splittogether.backend.expense.dto

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class ExpenseResponse(
    val id: Long,
    val groupId: Long,
    val title: String,
    val description: String?,
    val amount: BigDecimal,
    val currencyCode: String,
    val baseCurrencyCode: String,
    val categoryCode: String?,
    val splitMethod: String,
    val expenseDate: LocalDate,
    val paidByUserId: Long,
    val paidByName: String,
    val participants: List<ExpenseParticipantResponse>,
    val createdAt: Instant
)
