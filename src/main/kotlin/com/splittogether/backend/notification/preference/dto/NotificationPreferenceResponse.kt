package com.splittogether.backend.notification.preference.dto

data class NotificationPreferenceResponse(
    val notificationType: String,
    val notificationTypeName: String,
    val channel: String,
    val channelName: String,
    val enabled: Boolean
)
