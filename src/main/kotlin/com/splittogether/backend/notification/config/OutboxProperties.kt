package com.splittogether.backend.notification.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notification.outbox")
data class OutboxProperties(
    val pollDelay: Long = 1000,
    val batchSize: Int = 50,
    val maxAttempts: Int = 10,
    val schedulerEnabled: Boolean = true
)
