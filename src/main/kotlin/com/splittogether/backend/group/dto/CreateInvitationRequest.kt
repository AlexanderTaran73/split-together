package com.splittogether.backend.group.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.time.Instant

data class CreateInvitationRequest(
    @field:NotBlank
    val type: String,

    val invitedUserId: Long? = null,

    @field:Positive
    val maxUses: Int? = null,

    val expiresAt: Instant? = null
)
