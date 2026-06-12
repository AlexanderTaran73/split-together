package com.splittogether.backend.user.dto

data class UserPrivacyResponse(
    val searchVisibility: String,
    val groupInvitePolicy: String
)
