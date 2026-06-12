package com.splittogether.backend.friendship.dto

import java.time.Instant

data class FriendResponse(
    val userId: Long,
    val displayName: String,
    val avatarUrl: String?,
    val friendsSince: Instant?
)
