package com.splittogether.backend.notification.dispatch

import com.splittogether.backend.notification.entity.OutboxEvent
import com.splittogether.backend.notification.event.OutboxEventType
import com.splittogether.backend.notification.event.payload.GroupInvitationPayload
import com.splittogether.backend.notification.event.payload.VerificationCodePayload
import com.splittogether.backend.notification.push.PushMessage
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class NotificationDispatcher(
    private val objectMapper: ObjectMapper,
    private val emailNotificationHandler: EmailNotificationHandler,
    private val pushNotificationHandler: PushNotificationHandler
) {

    fun dispatch(event: OutboxEvent) {
        when (val type = OutboxEventType.valueOf(event.eventType)) {
            OutboxEventType.REGISTRATION_CODE,
            OutboxEventType.EMAIL_CHANGE_CODE,
            OutboxEventType.PASSWORD_RESET_CODE -> {
                val payload = objectMapper.readValue(event.payload, VerificationCodePayload::class.java)
                emailNotificationHandler.sendVerificationCode(type, payload)
            }

            OutboxEventType.GROUP_INVITATION_RECEIVED -> {
                val payload = objectMapper.readValue(event.payload, GroupInvitationPayload::class.java)
                pushNotificationHandler.send(
                    payload.invitedUserId,
                    PushMessage(
                        title = "Приглашение в группу",
                        body = "${payload.invitedByDisplayName} пригласил вас в группу «${payload.groupName}»",
                        data = mapOf("type" to type.name, "groupId" to payload.groupId.toString())
                    )
                )
            }
        }
    }
}
