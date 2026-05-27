package com.splittogether.backend.email.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EmailService {

    private val log = LoggerFactory.getLogger(javaClass)

    fun sendVerificationCode(email: String, code: String) {
        log.info("[EMAIL] Verification code for {}: {}", email, code)
    }
}
