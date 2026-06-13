package com.splittogether.backend.expense.dto

import java.math.BigDecimal

data class ExpenseParticipantResponse(
    val userId: Long,
    val displayName: String,
    val share: BigDecimal,
    val baseShare: BigDecimal,
    val weight: BigDecimal?,
    val confirmationStatus: String,
    val disputeReason: String?
)
