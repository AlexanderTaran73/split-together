package com.splittogether.backend.notification

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.notification.config.OutboxProperties
import com.splittogether.backend.notification.entity.OutboxEvent
import com.splittogether.backend.notification.event.OutboxEventType
import com.splittogether.backend.notification.event.payload.VerificationCodePayload
import com.splittogether.backend.notification.repository.OutboxEventRepository
import com.splittogether.backend.notification.service.OutboxService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestPropertySource(properties = ["email.enable-sending=true", "notification.outbox.max-attempts=3"])
class OutboxProcessorTest : AbstractIntegrationTest() {

    @Autowired private lateinit var outboxService: OutboxService
    @Autowired private lateinit var outboxEventRepository: OutboxEventRepository
    @Autowired private lateinit var outboxProperties: OutboxProperties

    @Test
    fun `append leaves the event pending until processed`() {
        outboxService.append(OutboxEventType.REGISTRATION_CODE, VerificationCodePayload("user@test.com", "123456"))

        val event = outboxEventRepository.findAll().single()
        assertNull(event.processedAt)
        assertEquals(0, capturingMailSender.messages.size)
    }

    @Test
    fun `processBatch dispatches a pending event and marks it processed`() {
        outboxService.append(OutboxEventType.REGISTRATION_CODE, VerificationCodePayload("user@test.com", "123456"))

        drainOutbox()

        assertEquals(1, capturingMailSender.messages.size)
        assertEquals("user@test.com", capturingMailSender.last().allRecipients[0].toString())
        assertNotNull(outboxEventRepository.findAll().single().processedAt)
    }

    @Test
    fun `processBatch does not re-dispatch an already processed event`() {
        outboxService.append(OutboxEventType.REGISTRATION_CODE, VerificationCodePayload("user@test.com", "123456"))

        drainOutbox()
        drainOutbox()

        assertEquals(1, capturingMailSender.messages.size)
    }

    @Test
    fun `a failing dispatch increments attempts and schedules a retry`() {
        val event = outboxEventRepository.save(OutboxEvent(eventType = "BOGUS_TYPE", payload = "{}"))

        drainOutbox()

        val reloaded = outboxEventRepository.findById(event.id).get()
        assertEquals(1, reloaded.attempts)
        assertNull(reloaded.processedAt)
        assertNull(reloaded.failedAt)
        assertTrue(reloaded.nextAttemptAt.isAfter(reloaded.createdAt))
    }

    @Test
    fun `a dispatch failing past max attempts is marked as permanently failed`() {
        val event = outboxEventRepository.save(
            OutboxEvent(eventType = "BOGUS_TYPE", payload = "{}", attempts = outboxProperties.maxAttempts - 1)
        )

        drainOutbox()

        val reloaded = outboxEventRepository.findById(event.id).get()
        assertEquals(outboxProperties.maxAttempts, reloaded.attempts)
        assertNotNull(reloaded.failedAt)
        assertNull(reloaded.processedAt)
    }
}
