package com.splittogether.backend.friendship.dto

import java.time.Instant

data class FriendRequestResponse(
    val id: Long,
    val userId: Long,
    val displayName: String,
    val avatarUrl: String?,
    val status: String,
    val createdAt: Instant
)
