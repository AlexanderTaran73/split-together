package com.splittogether.backend.settlement.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class CreateSettlementRequest(
    @field:NotNull
    val receiverId: Long,

    @field:NotNull
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    val amount: BigDecimal,

    @field:NotBlank
    val currencyCode: String,

    val note: String? = null
)
