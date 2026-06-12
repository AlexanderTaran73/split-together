package com.splittogether.backend.friendship.dto

import jakarta.validation.constraints.Positive

data class SendFriendRequest(
    @field:Positive
    val userId: Long
)
