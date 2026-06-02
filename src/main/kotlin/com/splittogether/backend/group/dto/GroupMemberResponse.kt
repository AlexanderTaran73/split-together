package com.splittogether.backend.group.dto

import java.time.Instant

data class GroupMemberResponse(
    val id: Long,
    val userId: Long,
    val displayName: String,
    val avatarUrl: String?,
    val role: String,
    val joinedAt: Instant?
)
