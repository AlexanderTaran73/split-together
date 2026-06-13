package com.splittogether.backend.notification.dispatch

import com.splittogether.backend.notification.preference.NotificationChannel
import com.splittogether.backend.notification.preference.NotificationPreferenceService
import com.splittogether.backend.notification.push.PushMessage
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class NotificationDelivery(
    private val preferenceService: NotificationPreferenceService,
    private val emailNotificationHandler: EmailNotificationHandler,
    private val pushNotificationHandler: PushNotificationHandler,
    private val userRepository: UserRepository
) {

    fun deliver(
        typeCode: String,
        recipientUserIds: List<Long>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        recipientUserIds.distinct().forEach { userId ->
            if (preferenceService.isEnabled(userId, typeCode, NotificationChannel.EMAIL)) {
                userRepository.findById(userId).ifPresent { user ->
                    emailNotificationHandler.sendNotification(user.email, title, body)
                }
            }
            if (preferenceService.isEnabled(userId, typeCode, NotificationChannel.PUSH)) {
                pushNotificationHandler.send(userId, PushMessage(title, body, data))
            }
        }
    }
}
