package com.splittogether.backend.notification.event.payload

data class GroupInvitationPayload(
    val invitedUserId: Long,
    val groupId: Long,
    val groupName: String,
    val invitedByDisplayName: String
)
