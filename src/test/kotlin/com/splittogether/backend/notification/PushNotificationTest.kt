package com.splittogether.backend.notification

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.group.dto.CreateGroupRequest
import com.splittogether.backend.group.dto.CreateInvitationRequest
import com.splittogether.backend.group.service.GroupService
import com.splittogether.backend.notification.device.DeviceTokenRepository
import com.splittogether.backend.notification.device.DeviceTokenService
import com.splittogether.backend.notification.device.dto.RegisterDeviceRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PushNotificationTest : AbstractIntegrationTest() {

    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var deviceTokenService: DeviceTokenService
    @Autowired private lateinit var deviceTokenRepository: DeviceTokenRepository

    @Test
    fun `direct invitation pushes to the invited user devices`() {
        val owner = generator.user(email = "owner@test.com", displayName = "Owner")
        val invited = generator.user(email = "invited@test.com")
        deviceTokenService.register(invited.id, RegisterDeviceRequest("invited-token", "ANDROID"))
        val group = groupService.createGroup(owner.id, CreateGroupRequest("Trip", null, "RUB"))

        groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("DIRECT", invitedUserId = invited.id))
        drainOutbox()

        assertEquals(1, capturingPushSender.sent.size)
        val sent = capturingPushSender.last()
        assertEquals(listOf("invited-token"), sent.tokens)
        assertTrue(sent.message.body.contains("Trip"))
        assertTrue(sent.message.body.contains("Owner"))
    }

    @Test
    fun `link invitation does not push`() {
        val owner = generator.user(email = "owner@test.com")
        val group = groupService.createGroup(owner.id, CreateGroupRequest("Trip", null, "RUB"))

        groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("LINK"))
        drainOutbox()

        assertTrue(capturingPushSender.sent.isEmpty())
    }

    @Test
    fun `invited user without devices yields no push send`() {
        val owner = generator.user(email = "owner@test.com")
        val invited = generator.user(email = "invited@test.com")
        val group = groupService.createGroup(owner.id, CreateGroupRequest("Trip", null, "RUB"))

        groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("DIRECT", invitedUserId = invited.id))
        drainOutbox()

        assertTrue(capturingPushSender.sent.isEmpty())
    }

    @Test
    fun `tokens reported stale by the sender are pruned`() {
        val owner = generator.user(email = "owner@test.com")
        val invited = generator.user(email = "invited@test.com")
        deviceTokenService.register(invited.id, RegisterDeviceRequest("stale-token", "ANDROID"))
        capturingPushSender.staleTokens = listOf("stale-token")
        val group = groupService.createGroup(owner.id, CreateGroupRequest("Trip", null, "RUB"))

        groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("DIRECT", invitedUserId = invited.id))
        drainOutbox()

        assertTrue(deviceTokenRepository.findByUserId(invited.id).isEmpty())
    }
}
