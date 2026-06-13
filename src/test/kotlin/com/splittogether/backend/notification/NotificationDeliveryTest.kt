package com.splittogether.backend.notification

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.expense.dto.CreateExpenseRequest
import com.splittogether.backend.expense.dto.ParticipantRequest
import com.splittogether.backend.expense.service.ExpenseService
import com.splittogether.backend.group.dto.CreateGroupRequest
import com.splittogether.backend.group.dto.CreateInvitationRequest
import com.splittogether.backend.group.dto.JoinGroupRequest
import com.splittogether.backend.group.service.GroupService
import com.splittogether.backend.notification.device.DeviceTokenService
import com.splittogether.backend.notification.device.dto.RegisterDeviceRequest
import com.splittogether.backend.notification.preference.NotificationChannel
import com.splittogether.backend.notification.preference.NotificationPreferenceService
import com.splittogether.backend.notification.preference.NotificationType
import com.splittogether.backend.notification.preference.dto.NotificationPreferenceItem
import com.splittogether.backend.notification.preference.dto.UpdateNotificationPreferencesRequest
import com.splittogether.backend.settlement.dto.CreateSettlementRequest
import com.splittogether.backend.settlement.service.SettlementService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestPropertySource(properties = ["email.enable-sending=true"])
class NotificationDeliveryTest : AbstractIntegrationTest() {

    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var expenseService: ExpenseService
    @Autowired private lateinit var settlementService: SettlementService
    @Autowired private lateinit var deviceTokenService: DeviceTokenService
    @Autowired private lateinit var preferenceService: NotificationPreferenceService

    private fun groupId(ownerId: Long): Long =
        groupService.createGroup(ownerId, CreateGroupRequest("Test Group", null, "RUB")).id

    private fun join(userId: Long, ownerId: Long, gid: Long) {
        val inv = groupService.createInvitation(ownerId, gid, CreateInvitationRequest("LINK"))
        groupService.joinGroup(userId, JoinGroupRequest(inv.token!!))
    }

    private fun registerToken(userId: Long, token: String) =
        deviceTokenService.register(userId, RegisterDeviceRequest(token, "ANDROID"))

    private fun pushedTokens(): List<String> = capturingPushSender.sent.flatMap { it.tokens }

    @Test
    fun `expense creation pushes to other participants but not the creator`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val gid = groupId(owner.id)
        join(member.id, owner.id, gid)
        registerToken(owner.id, "owner-tok")
        registerToken(member.id, "member-tok")

        expenseService.createExpense(
            owner.id, gid,
            CreateExpenseRequest(
                title = "Dinner", amount = BigDecimal("100.00"), currencyCode = "RUB",
                splitMethod = "EQUAL", expenseDate = LocalDate.now(),
                paidByUserId = owner.id, participants = listOf(ParticipantRequest(owner.id), ParticipantRequest(member.id))
            )
        )
        drainOutbox()

        val tokens = pushedTokens()
        assertTrue("member-tok" in tokens, "member should be notified")
        assertFalse("owner-tok" in tokens, "creator should not be notified")
    }

    @Test
    fun `settlement requested pushes to the receiver`() {
        val payer = generator.user(email = "payer@test.com")
        val receiver = generator.user(email = "receiver@test.com")
        val gid = groupId(payer.id)
        join(receiver.id, payer.id, gid)
        registerToken(receiver.id, "receiver-tok")

        settlementService.createSettlement(
            payer.id, gid, CreateSettlementRequest(receiverId = receiver.id, amount = BigDecimal("50.00"), currencyCode = "RUB")
        )
        drainOutbox()

        assertTrue("receiver-tok" in pushedTokens())
    }

    @Test
    fun `settlement confirmation pushes to the payer`() {
        val payer = generator.user(email = "payer@test.com")
        val receiver = generator.user(email = "receiver@test.com")
        val gid = groupId(payer.id)
        join(receiver.id, payer.id, gid)
        registerToken(payer.id, "payer-tok")
        val settlement = settlementService.createSettlement(
            payer.id, gid, CreateSettlementRequest(receiverId = receiver.id, amount = BigDecimal("50.00"), currencyCode = "RUB")
        )
        capturingPushSender.clear()

        settlementService.confirmSettlement(receiver.id, gid, settlement.id)
        drainOutbox()

        assertTrue("payer-tok" in pushedTokens())
    }

    @Test
    fun `disabling push for a type suppresses push while email is still sent`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val gid = groupId(owner.id)
        join(member.id, owner.id, gid)
        registerToken(member.id, "member-tok")
        preferenceService.updatePreferences(
            member.id,
            UpdateNotificationPreferencesRequest(
                listOf(NotificationPreferenceItem(NotificationType.EXPENSE_ADDED, NotificationChannel.PUSH, false))
            )
        )

        expenseService.createExpense(
            owner.id, gid,
            CreateExpenseRequest(
                title = "Dinner", amount = BigDecimal("100.00"), currencyCode = "RUB",
                splitMethod = "EQUAL", expenseDate = LocalDate.now(),
                paidByUserId = owner.id, participants = listOf(ParticipantRequest(owner.id), ParticipantRequest(member.id))
            )
        )
        drainOutbox()

        assertFalse("member-tok" in pushedTokens(), "push suppressed by preference")
        assertEquals(1, capturingMailSender.messages.size, "email still sent")
        assertEquals("member@test.com", capturingMailSender.last().allRecipients[0].toString())
    }
}
