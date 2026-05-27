package com.splittogether.backend.user.dto

import java.time.Instant

data class UserResponse(
    val id: Long,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val createdAt: Instant
)
