package com.splittogether.backend.settlement.dto

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class SettlementResponse(
    val id: Long,
    val groupId: Long,
    val payerId: Long,
    val payerName: String,
    val receiverId: Long,
    val receiverName: String,
    val amount: BigDecimal,
    val currencyCode: String,
    val baseAmount: BigDecimal?,
    val baseCurrencyCode: String,
    val settlementDate: LocalDate,
    val status: String,
    val note: String?,
    val rejectionReason: String?,
    val createdAt: Instant,
    val confirmedAt: Instant?,
    val rejectedAt: Instant?
)
