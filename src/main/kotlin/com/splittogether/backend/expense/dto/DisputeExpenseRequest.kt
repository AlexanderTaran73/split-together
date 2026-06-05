package com.splittogether.backend.expense.dto

import jakarta.validation.constraints.NotBlank

data class DisputeExpenseRequest(
    @field:NotBlank
    val reason: String
)
