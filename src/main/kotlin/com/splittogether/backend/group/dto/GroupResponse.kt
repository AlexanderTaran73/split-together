package com.splittogether.backend.group.dto

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
    val createdAt: Instant
)
