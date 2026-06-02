package com.splittogether.backend.group.dto

import jakarta.validation.constraints.NotBlank

data class JoinGroupRequest(
    @field:NotBlank
    val inviteCode: String
)
