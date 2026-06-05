package com.splittogether.backend.group.dto

import java.time.Instant

data class IncomingInvitationResponse(
    val id: Long,
    val groupId: Long,
    val groupName: String,
    val groupCurrencyCode: String,
    val invitedById: Long,
    val invitedByDisplayName: String,
    val expiresAt: Instant?,
    val createdAt: Instant
)
