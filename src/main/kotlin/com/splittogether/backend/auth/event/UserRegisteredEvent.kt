package com.splittogether.backend.auth.event

data class UserRegisteredEvent(
    val email: String,
    val code: String
)
