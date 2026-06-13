package com.splittogether.backend.notification.preference.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class UpdateNotificationPreferencesRequest(
    @field:NotEmpty(message = "preferences must not be empty")
    @field:Valid
    val preferences: List<NotificationPreferenceItem>
)

data class NotificationPreferenceItem(
    @field:NotBlank(message = "notificationType is required")
    val notificationType: String,

    @field:NotBlank(message = "channel is required")
    val channel: String,

    val enabled: Boolean
)
