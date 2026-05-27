package com.splittogether.backend.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 50)
    val displayName: String
)
