package com.splittogether.backend.group.dto

import java.math.BigDecimal
import java.time.Instant

data class GroupResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val currencyCode: String,
    val status: String,
    val ownerId: Long,
    val ownerDisplayName: String,
    val memberCount: Long,
    val expenseCount: Long,
    val currentUserRole: String,
    val currentUserBalance: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant,
    val archivedAt: Instant?
)
