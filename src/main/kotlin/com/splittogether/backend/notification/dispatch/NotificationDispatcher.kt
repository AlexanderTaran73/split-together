package com.splittogether.backend.notification.dispatch

import com.splittogether.backend.notification.entity.OutboxEvent
import com.splittogether.backend.notification.event.OutboxEventType
import com.splittogether.backend.notification.event.payload.ExpenseAddedPayload
import com.splittogether.backend.notification.event.payload.GroupInvitationPayload
import com.splittogether.backend.notification.event.payload.SettlementConfirmedPayload
import com.splittogether.backend.notification.event.payload.SettlementRequestedPayload
import com.splittogether.backend.notification.event.payload.VerificationCodePayload
import com.splittogether.backend.notification.preference.NotificationType
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class NotificationDispatcher(
    private val objectMapper: ObjectMapper,
    private val emailNotificationHandler: EmailNotificationHandler,
    private val notificationDelivery: NotificationDelivery
) {

    fun dispatch(event: OutboxEvent) {
        when (val type = OutboxEventType.valueOf(event.eventType)) {
            // Verification/security emails — always sent, not subject to user preferences.
            OutboxEventType.REGISTRATION_CODE,
            OutboxEventType.EMAIL_CHANGE_CODE,
            OutboxEventType.PASSWORD_RESET_CODE -> {
                val payload = objectMapper.readValue(event.payload, VerificationCodePayload::class.java)
                emailNotificationHandler.sendVerificationCode(type, payload)
            }

            OutboxEventType.GROUP_INVITATION_RECEIVED -> {
                val p = objectMapper.readValue(event.payload, GroupInvitationPayload::class.java)
                notificationDelivery.deliver(
                    typeCode = NotificationType.GROUP_INVITATION,
                    recipientUserIds = listOf(p.invitedUserId),
                    title = "Приглашение в группу",
                    body = "${p.invitedByDisplayName} пригласил вас в группу «${p.groupName}»",
                    data = mapOf("type" to NotificationType.GROUP_INVITATION, "groupId" to p.groupId.toString())
                )
            }

            OutboxEventType.EXPENSE_ADDED -> {
                val p = objectMapper.readValue(event.payload, ExpenseAddedPayload::class.java)
                notificationDelivery.deliver(
                    typeCode = NotificationType.EXPENSE_ADDED,
                    recipientUserIds = p.recipientUserIds,
                    title = "Новая трата",
                    body = "${p.actorName} добавил трату «${p.expenseTitle}» на ${p.amount} ${p.currencyCode} в группе «${p.groupName}»",
                    data = mapOf("type" to NotificationType.EXPENSE_ADDED, "groupId" to p.groupId.toString())
                )
            }

            OutboxEventType.SETTLEMENT_REQUESTED -> {
                val p = objectMapper.readValue(event.payload, SettlementRequestedPayload::class.java)
                notificationDelivery.deliver(
                    typeCode = NotificationType.SETTLEMENT_REQUESTED,
                    recipientUserIds = listOf(p.recipientUserId),
                    title = "Подтвердите пополнение",
                    body = "${p.payerName} отметил пополнение на ${p.amount} ${p.currencyCode} в группе «${p.groupName}» — подтвердите получение",
                    data = mapOf("type" to NotificationType.SETTLEMENT_REQUESTED, "groupId" to p.groupId.toString())
                )
            }

            OutboxEventType.SETTLEMENT_CONFIRMED -> {
                val p = objectMapper.readValue(event.payload, SettlementConfirmedPayload::class.java)
                notificationDelivery.deliver(
                    typeCode = NotificationType.SETTLEMENT_CONFIRMED,
                    recipientUserIds = listOf(p.recipientUserId),
                    title = "Пополнение подтверждено",
                    body = "${p.receiverName} подтвердил ваше пополнение на ${p.amount} ${p.currencyCode} в группе «${p.groupName}»",
                    data = mapOf("type" to NotificationType.SETTLEMENT_CONFIRMED, "groupId" to p.groupId.toString())
                )
            }
        }
    }
}
