package com.splittogether.backend.group.dto

import jakarta.validation.constraints.NotBlank

data class UpdateMemberRoleRequest(
    @field:NotBlank
    val role: String
)
