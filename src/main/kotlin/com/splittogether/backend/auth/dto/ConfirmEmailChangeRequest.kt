package com.splittogether.backend.auth.dto

import jakarta.validation.constraints.NotBlank

data class ConfirmEmailChangeRequest(
    @field:NotBlank
    val code: String
)
