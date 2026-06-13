package com.splittogether.backend.notification

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.common.exception.InvalidNotificationPreferenceException
import com.splittogether.backend.notification.preference.NotificationChannel
import com.splittogether.backend.notification.preference.NotificationPreferenceRepository
import com.splittogether.backend.notification.preference.NotificationPreferenceService
import com.splittogether.backend.notification.preference.NotificationType
import com.splittogether.backend.notification.preference.dto.NotificationPreferenceItem
import com.splittogether.backend.notification.preference.dto.UpdateNotificationPreferencesRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPreferenceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var service: NotificationPreferenceService
    @Autowired private lateinit var preferenceRepository: NotificationPreferenceRepository

    @Test
    fun `isEnabled defaults to true when nothing is configured (opt-out)`() {
        val user = generator.user(email = "user@test.com")

        assertTrue(service.isEnabled(user.id, NotificationType.EXPENSE_ADDED, NotificationChannel.PUSH))
        assertTrue(service.isEnabled(user.id, NotificationType.GROUP_INVITATION, NotificationChannel.EMAIL))
    }

    @Test
    fun `getPreferences returns the full type x channel matrix, all enabled by default`() {
        val user = generator.user(email = "user@test.com")

        val prefs = service.getPreferences(user.id)

        // 4 types x 2 channels
        assertEquals(8, prefs.size)
        assertTrue(prefs.all { it.enabled })
    }

    @Test
    fun `update disables a single type-channel and persists across reads`() {
        val user = generator.user(email = "user@test.com")

        service.updatePreferences(
            user.id,
            UpdateNotificationPreferencesRequest(
                listOf(NotificationPreferenceItem(NotificationType.EXPENSE_ADDED, NotificationChannel.PUSH, false))
            )
        )

        assertFalse(service.isEnabled(user.id, NotificationType.EXPENSE_ADDED, NotificationChannel.PUSH))
        // other combinations remain enabled
        assertTrue(service.isEnabled(user.id, NotificationType.EXPENSE_ADDED, NotificationChannel.EMAIL))
        assertTrue(service.isEnabled(user.id, NotificationType.GROUP_INVITATION, NotificationChannel.PUSH))
    }

    @Test
    fun `update is idempotent upsert - second update flips the same row`() {
        val user = generator.user(email = "user@test.com")

        service.updatePreferences(
            user.id,
            UpdateNotificationPreferencesRequest(
                listOf(NotificationPreferenceItem(NotificationType.SETTLEMENT_REQUESTED, NotificationChannel.EMAIL, false))
            )
        )
        service.updatePreferences(
            user.id,
            UpdateNotificationPreferencesRequest(
                listOf(NotificationPreferenceItem(NotificationType.SETTLEMENT_REQUESTED, NotificationChannel.EMAIL, true))
            )
        )

        assertTrue(service.isEnabled(user.id, NotificationType.SETTLEMENT_REQUESTED, NotificationChannel.EMAIL))
        assertEquals(1, preferenceRepository.findByUserId(user.id).size)
    }

    @Test
    fun `update rejects an unknown notification type`() {
        val user = generator.user(email = "user@test.com")

        assertFailsWith<InvalidNotificationPreferenceException> {
            service.updatePreferences(
                user.id,
                UpdateNotificationPreferencesRequest(
                    listOf(NotificationPreferenceItem("NOPE", NotificationChannel.PUSH, false))
                )
            )
        }
    }
}
