package com.splittogether.backend.expense.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class UpdateExpenseRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,

    @field:Size(max = 1000)
    val description: String? = null,

    @field:NotNull
    @field:DecimalMin("0.01")
    val amount: BigDecimal,

    @field:NotBlank
    val currencyCode: String,

    val categoryCode: String? = null,

    @field:NotBlank
    val splitMethod: String,

    @field:NotNull
    val expenseDate: LocalDate,

    @field:NotNull
    val paidByUserId: Long,

    @field:NotEmpty
    val participants: List<ParticipantRequest>
)
