package com.splittogether.backend.group.dto

import java.time.Instant

data class GroupInvitationResponse(
    val id: Long,
    val type: String,
    val status: String,
    val inviteCode: String?,
    val invitedUserId: Long?,
    val maxUses: Int?,
    val usesCount: Long,
    val expiresAt: Instant?,
    val createdAt: Instant
)
