package com.splittogether.backend.notification.dispatch

import com.splittogether.backend.notification.entity.OutboxEvent
import com.splittogether.backend.notification.event.OutboxEventType
import com.splittogether.backend.notification.event.payload.VerificationCodePayload
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class NotificationDispatcher(
    private val objectMapper: ObjectMapper,
    private val emailNotificationHandler: EmailNotificationHandler
) {

    fun dispatch(event: OutboxEvent) {
        when (val type = OutboxEventType.valueOf(event.eventType)) {
            OutboxEventType.REGISTRATION_CODE,
            OutboxEventType.EMAIL_CHANGE_CODE,
            OutboxEventType.PASSWORD_RESET_CODE -> {
                val payload = objectMapper.readValue(event.payload, VerificationCodePayload::class.java)
                emailNotificationHandler.sendVerificationCode(type, payload)
            }
        }
    }
}
