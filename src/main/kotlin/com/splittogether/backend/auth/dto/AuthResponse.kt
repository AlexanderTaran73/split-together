package com.splittogether.backend.auth.dto

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String
)
