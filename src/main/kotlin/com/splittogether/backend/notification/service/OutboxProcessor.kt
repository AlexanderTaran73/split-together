package com.splittogether.backend.notification.service

import com.splittogether.backend.notification.config.OutboxProperties
import com.splittogether.backend.notification.dispatch.NotificationDispatcher
import com.splittogether.backend.notification.repository.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class OutboxProcessor(
    private val outboxEventRepository: OutboxEventRepository,
    private val dispatcher: NotificationDispatcher,
    private val properties: OutboxProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun processBatch() {
        val batch = outboxEventRepository.findProcessable(Instant.now(), PageRequest.of(0, properties.batchSize))
        for (event in batch) {
            try {
                dispatcher.dispatch(event)
                event.processedAt = Instant.now()
            } catch (e: Exception) {
                event.attempts += 1
                if (event.attempts >= properties.maxAttempts) {
                    event.failedAt = Instant.now()
                    log.error(
                        "Outbox event {} (type={}) failed permanently after {} attempts: {}",
                        event.id, event.eventType, event.attempts, e.message, e
                    )
                } else {
                    val backoffSeconds = minOf(1L shl event.attempts, 3600L)
                    event.nextAttemptAt = Instant.now().plusSeconds(backoffSeconds)
                    log.warn(
                        "Outbox event {} (type={}) dispatch failed (attempt {}), retrying in {}s: {}",
                        event.id, event.eventType, event.attempts, backoffSeconds, e.message
                    )
                }
            }
        }
    }
}
