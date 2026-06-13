package com.splittogether.backend.notification.service

import com.splittogether.backend.notification.entity.OutboxEvent
import com.splittogether.backend.notification.event.OutboxEventType
import com.splittogether.backend.notification.repository.OutboxEventRepository
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class OutboxService(
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper
) {

    fun append(type: OutboxEventType, payload: Any) {
        outboxEventRepository.save(
            OutboxEvent(
                eventType = type.name,
                payload = objectMapper.writeValueAsString(payload)
            )
        )
    }
}
