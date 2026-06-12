package com.splittogether.backend.user.dto

import jakarta.validation.constraints.NotBlank

data class UpdatePrivacyRequest(
    @field:NotBlank
    val searchVisibility: String,

    @field:NotBlank
    val groupInvitePolicy: String
)
