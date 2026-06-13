package com.splittogether.backend.balance.dto

import java.math.BigDecimal

data class SimplifiedDebtResponse(
    val fromUserId: Long,
    val fromName: String,
    val toUserId: Long,
    val toName: String,
    val amount: BigDecimal,
    val currencyCode: String
)
