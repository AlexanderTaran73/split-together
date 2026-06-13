package com.splittogether.backend.notification.dispatch

import com.splittogether.backend.notification.device.DeviceTokenRepository
import com.splittogether.backend.notification.push.PushMessage
import com.splittogether.backend.notification.push.PushSender
import org.springframework.stereotype.Component

@Component
class PushNotificationHandler(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val pushSender: PushSender
) {

    fun send(userId: Long, message: PushMessage) {
        val tokens = deviceTokenRepository.findByUserId(userId).map { it.token }
        if (tokens.isEmpty()) return

        val staleTokens = pushSender.send(tokens, message)
        if (staleTokens.isNotEmpty()) deviceTokenRepository.deleteByTokenIn(staleTokens)
    }
}
