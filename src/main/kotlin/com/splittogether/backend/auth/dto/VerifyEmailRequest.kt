package com.splittogether.backend.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class VerifyEmailRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val code: String
)
