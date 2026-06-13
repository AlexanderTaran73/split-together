package com.splittogether.backend.notification.device

import com.splittogether.backend.common.exception.InvalidDevicePlatformException
import com.splittogether.backend.notification.device.dto.RegisterDeviceRequest
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class DeviceTokenService(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val devicePlatformRepository: DevicePlatformRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun register(userId: Long, request: RegisterDeviceRequest) {
        val platform = devicePlatformRepository.findByCode(request.platform.uppercase())
            ?: throw InvalidDevicePlatformException("Unknown device platform: ${request.platform}")

        val existing = deviceTokenRepository.findByToken(request.token)
        if (existing != null) {
            existing.user = userRepository.getReferenceById(userId)
            existing.platform = platform
            existing.lastUsedAt = Instant.now()
        } else {
            deviceTokenRepository.save(
                DeviceToken(
                    user = userRepository.getReferenceById(userId),
                    platform = platform,
                    token = request.token
                )
            )
        }
    }

    @Transactional
    fun revoke(userId: Long, token: String) {
        val deviceToken = deviceTokenRepository.findByToken(token) ?: return
        if (deviceToken.user.id == userId) deviceTokenRepository.delete(deviceToken)
    }
}
