package com.splittogether.backend.settlement.dto

import java.math.BigDecimal
import java.time.Instant

data class SettlementResponse(
    val id: Long,
    val groupId: Long,
    val payerId: Long,
    val payerName: String,
    val receiverId: Long,
    val receiverName: String,
    val amount: BigDecimal,
    val currencyCode: String,
    val status: String,
    val createdAt: Instant,
    val confirmedAt: Instant?,
    val rejectedAt: Instant?
)
