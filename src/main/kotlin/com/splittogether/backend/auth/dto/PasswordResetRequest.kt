package com.splittogether.backend.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class PasswordResetRequest(
    @field:Email @field:NotBlank val email: String
)
