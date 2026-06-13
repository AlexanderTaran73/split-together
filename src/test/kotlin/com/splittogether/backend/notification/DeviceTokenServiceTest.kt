package com.splittogether.backend.notification

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.common.exception.InvalidDevicePlatformException
import com.splittogether.backend.notification.device.DeviceTokenRepository
import com.splittogether.backend.notification.device.DeviceTokenService
import com.splittogether.backend.notification.device.dto.RegisterDeviceRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceTokenServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var deviceTokenService: DeviceTokenService
    @Autowired private lateinit var deviceTokenRepository: DeviceTokenRepository

    @Test
    fun `register stores a device token for the user`() {
        val user = generator.user(email = "user@test.com")

        deviceTokenService.register(user.id, RegisterDeviceRequest("token-1", "ANDROID"))

        val tokens = deviceTokenRepository.findByUserId(user.id)
        assertEquals(1, tokens.size)
        assertEquals("token-1", tokens.single().token)
    }

    @Test
    fun `registering the same token again reassigns it to the current user`() {
        val first = generator.user(email = "first@test.com")
        val second = generator.user(email = "second@test.com")

        deviceTokenService.register(first.id, RegisterDeviceRequest("shared-token", "ANDROID"))
        deviceTokenService.register(second.id, RegisterDeviceRequest("shared-token", "ANDROID"))

        assertTrue(deviceTokenRepository.findByUserId(first.id).isEmpty())
        assertEquals(1, deviceTokenRepository.findByUserId(second.id).size)
    }

    @Test
    fun `register rejects an unknown platform`() {
        val user = generator.user(email = "user@test.com")

        assertFailsWith<InvalidDevicePlatformException> {
            deviceTokenService.register(user.id, RegisterDeviceRequest("token-1", "SYMBIAN"))
        }
    }

    @Test
    fun `revoke removes the caller's token`() {
        val user = generator.user(email = "user@test.com")
        deviceTokenService.register(user.id, RegisterDeviceRequest("token-1", "ANDROID"))

        deviceTokenService.revoke(user.id, "token-1")

        assertTrue(deviceTokenRepository.findByUserId(user.id).isEmpty())
    }

    @Test
    fun `revoke does not remove a token owned by another user`() {
        val owner = generator.user(email = "owner@test.com")
        val other = generator.user(email = "other@test.com")
        deviceTokenService.register(owner.id, RegisterDeviceRequest("token-1", "ANDROID"))

        deviceTokenService.revoke(other.id, "token-1")

        assertEquals(1, deviceTokenRepository.findByUserId(owner.id).size)
    }
}
