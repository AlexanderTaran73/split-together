package com.splittogether.backend.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PasswordResetConfirmRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val code: String,
    @field:NotBlank @field:Size(min = 8) val newPassword: String
)
