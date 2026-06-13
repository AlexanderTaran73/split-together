package com.splittogether.backend.notification.dispatch

import com.splittogether.backend.email.service.EmailService
import com.splittogether.backend.notification.event.OutboxEventType
import com.splittogether.backend.notification.event.payload.VerificationCodePayload
import org.springframework.stereotype.Component

@Component
class EmailNotificationHandler(private val emailService: EmailService) {

    fun sendVerificationCode(type: OutboxEventType, payload: VerificationCodePayload) {
        val subject = when (type) {
            OutboxEventType.PASSWORD_RESET_CODE -> "Password Reset Code — SplitTogether"
            else -> "Verification Code — SplitTogether"
        }
        emailService.send {
            to(payload.email)
            subject(subject)
            template("verification-code")
            variable("code", payload.code)
        }
    }
}
