package com.splittogether.backend.email.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "email")
data class EmailProperties(
    val from: String = "noreply@splittogether.com",
    val enableSending: Boolean = true
)
