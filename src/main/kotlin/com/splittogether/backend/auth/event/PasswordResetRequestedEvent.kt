package com.splittogether.backend.auth.event

data class PasswordResetRequestedEvent(
    val email: String,
    val code: String
)
