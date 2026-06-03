package com.splittogether.backend.user.dto

import java.math.BigDecimal

data class UserBalanceResponse(
    val totalOwed: BigDecimal,
    val totalOwing: BigDecimal,
    val netBalance: BigDecimal
)
