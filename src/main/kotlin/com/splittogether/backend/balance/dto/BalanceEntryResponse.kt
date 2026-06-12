package com.splittogether.backend.balance.dto

import java.math.BigDecimal

data class BalanceEntryResponse(
    val debtorId: Long,
    val debtorName: String,
    val creditorId: Long,
    val creditorName: String,
    val amount: BigDecimal
)
