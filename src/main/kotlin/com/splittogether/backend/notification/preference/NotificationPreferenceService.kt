package com.splittogether.backend.notification.preference

import com.splittogether.backend.common.exception.InvalidNotificationPreferenceException
import com.splittogether.backend.common.exception.UserNotFoundException
import com.splittogether.backend.notification.preference.dto.NotificationPreferenceResponse
import com.splittogether.backend.notification.preference.dto.UpdateNotificationPreferencesRequest
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationPreferenceService(
    private val preferenceRepository: NotificationPreferenceRepository,
    private val typeRepository: NotificationTypeRepository,
    private val channelRepository: NotificationChannelRepository,
    private val userRepository: UserRepository
) {

    /** Opt-out: при отсутствии явной настройки уведомление считается включённым. */
    @Transactional(readOnly = true)
    fun isEnabled(userId: Long, typeCode: String, channelCode: String): Boolean =
        preferenceRepository
            .findByUserIdAndNotificationTypeCodeAndChannelCode(userId, typeCode, channelCode)
            ?.enabled ?: true

    @Transactional(readOnly = true)
    fun getPreferences(userId: Long): List<NotificationPreferenceResponse> {
        val overrides = preferenceRepository.findByUserId(userId)
            .associateBy { it.notificationType.code to it.channel.code }
        val channels = channelRepository.findAll()
        return typeRepository.findAll().flatMap { type ->
            channels.map { channel ->
                NotificationPreferenceResponse(
                    notificationType = type.code,
                    notificationTypeName = type.name,
                    channel = channel.code,
                    channelName = channel.name,
                    enabled = overrides[type.code to channel.code]?.enabled ?: true
                )
            }
        }
    }

    @Transactional
    fun updatePreferences(userId: Long, request: UpdateNotificationPreferencesRequest): List<NotificationPreferenceResponse> {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException("User not found") }
        request.preferences.forEach { item ->
            val type = typeRepository.findByCode(item.notificationType)
                ?: throw InvalidNotificationPreferenceException("Unknown notification type: ${item.notificationType}")
            val channel = channelRepository.findByCode(item.channel)
                ?: throw InvalidNotificationPreferenceException("Unknown channel: ${item.channel}")

            val existing = preferenceRepository
                .findByUserIdAndNotificationTypeCodeAndChannelCode(userId, item.notificationType, item.channel)
            if (existing != null) {
                existing.enabled = item.enabled
            } else {
                preferenceRepository.save(
                    NotificationPreference(user = user, notificationType = type, channel = channel, enabled = item.enabled)
                )
            }
        }
        return getPreferences(userId)
    }
}
