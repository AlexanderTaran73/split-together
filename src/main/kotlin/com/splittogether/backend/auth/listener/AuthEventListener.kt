package com.splittogether.backend.auth.listener

import com.splittogether.backend.auth.event.EmailChangeRequestedEvent
import com.splittogether.backend.auth.event.UserRegisteredEvent
import com.splittogether.backend.email.service.EmailService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AuthEventListener(private val emailService: EmailService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onUserRegistered(event: UserRegisteredEvent) {
        try {
            emailService.send {
                to(event.email)
                subject("Verification Code — SplitTogether")
                template("verification-code")
                variable("code", event.code)
            }
        } catch (e: Exception) {
            log.error("Failed to send registration email to '{}': {}", event.email, e.message, e)
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onEmailChangeRequested(event: EmailChangeRequestedEvent) {
        try {
            emailService.send {
                to(event.newEmail)
                subject("Verification Code — SplitTogether")
                template("verification-code")
                variable("code", event.code)
            }
        } catch (e: Exception) {
            log.error("Failed to send email-change email to '{}': {}", event.newEmail, e.message, e)
        }
    }
}
