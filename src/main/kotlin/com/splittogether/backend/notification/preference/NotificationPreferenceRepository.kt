package com.splittogether.backend.notification.preference

import org.springframework.data.jpa.repository.JpaRepository

interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, Long> {
    fun findByUserId(userId: Long): List<NotificationPreference>
    fun findByUserIdAndNotificationTypeCodeAndChannelCode(
        userId: Long,
        notificationTypeCode: String,
        channelCode: String
    ): NotificationPreference?
}
