package com.splittogether.backend.group.dto

import java.time.Instant

data class GroupInvitationResponse(
    val id: Long,
    val type: String,
    val status: String,
    val token: String?,
    val targetUserId: Long?,
    val targetEmail: String?,
    val maxUses: Int?,
    val usedCount: Int,
    val expiresAt: Instant?,
    val createdAt: Instant
)
