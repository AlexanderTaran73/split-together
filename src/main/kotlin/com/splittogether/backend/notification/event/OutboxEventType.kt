package com.splittogether.backend.notification.event

enum class OutboxEventType {
    REGISTRATION_CODE,
    EMAIL_CHANGE_CODE,
    PASSWORD_RESET_CODE,
    GROUP_INVITATION_RECEIVED
}
