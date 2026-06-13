package com.splittogether.backend.notification.event.payload

data class VerificationCodePayload(
    val email: String,
    val code: String
)
