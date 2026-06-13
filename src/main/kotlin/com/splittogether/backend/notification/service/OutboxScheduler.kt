package com.splittogether.backend.notification.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "notification.outbox",
    name = ["scheduler-enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class OutboxScheduler(private val outboxProcessor: OutboxProcessor) {

    @Scheduled(fixedDelayString = "\${notification.outbox.poll-delay:1000}")
    fun poll() = outboxProcessor.processBatch()
}
