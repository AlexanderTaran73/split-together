package com.splittogether.backend.group.controller

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.LoginRequest
import com.splittogether.backend.auth.service.AuthService
import com.splittogether.backend.group.dto.CreateGroupRequest
import com.splittogether.backend.group.dto.CreateInvitationRequest
import com.splittogether.backend.group.entity.Group
import com.splittogether.backend.group.entity.GroupInvitation
import com.splittogether.backend.group.repository.GroupInvitationRepository
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.group.service.GroupService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class InvitationControllerTest : AbstractIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var groupRepository: GroupRepository
    @Autowired private lateinit var groupInvitationRepository: GroupInvitationRepository

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun token(email: String): String =
        authService.login(LoginRequest(email, generator.defaultPassword)).accessToken

    private fun createGroup(userId: Long): Group {
        groupService.createGroup(userId, CreateGroupRequest("Test Group", null, "RUB"))
        return groupRepository.findAll().last()
    }

    private fun createDirectInvitation(ownerId: Long, groupId: Long, invitedUserId: Long): GroupInvitation {
        groupService.createInvitation(ownerId, groupId, CreateInvitationRequest("DIRECT", invitedUserId = invitedUserId))
        return groupInvitationRepository.findAll().last()
    }

    // ── POST /invitations/{id}/accept ─────────────────────────────────────────

    @Test
    fun `POST accept returns 200 and adds invited user to group`() {
        val owner = generator.user(email = "owner@test.com")
        val invited = generator.user(email = "invited@test.com")
        val group = createGroup(owner.id)
        val inv = createDirectInvitation(owner.id, group.id, invited.id)

        mockMvc.perform(
            post("/api/v1/invitations/${inv.id}/accept")
                .header("Authorization", "Bearer ${token("invited@test.com")}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(group.id))
    }

    @Test
    fun `POST accept returns 400 when invitation belongs to another user`() {
        val owner = generator.user(email = "owner@test.com")
        val invited = generator.user(email = "invited@test.com")
        val other = generator.user(email = "other@test.com")
        val group = createGroup(owner.id)
        val inv = createDirectInvitation(owner.id, group.id, invited.id)

        mockMvc.perform(
            post("/api/v1/invitations/${inv.id}/accept")
                .header("Authorization", "Bearer ${token("other@test.com")}")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST accept returns 404 for unknown invitation`() {
        generator.user(email = "user@test.com")

        mockMvc.perform(
            post("/api/v1/invitations/999/accept")
                .header("Authorization", "Bearer ${token("user@test.com")}")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `POST accept returns 401 without token`() {
        mockMvc.perform(post("/api/v1/invitations/1/accept"))
            .andExpect(status().isUnauthorized)
    }

    // ── POST /invitations/{id}/reject ─────────────────────────────────────────

    @Test
    fun `POST reject returns 204 for invited user`() {
        val owner = generator.user(email = "owner@test.com")
        val invited = generator.user(email = "invited@test.com")
        val group = createGroup(owner.id)
        val inv = createDirectInvitation(owner.id, group.id, invited.id)

        mockMvc.perform(
            post("/api/v1/invitations/${inv.id}/reject")
                .header("Authorization", "Bearer ${token("invited@test.com")}")
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `POST reject returns 400 when invitation belongs to another user`() {
        val owner = generator.user(email = "owner@test.com")
        val invited = generator.user(email = "invited@test.com")
        val other = generator.user(email = "other@test.com")
        val group = createGroup(owner.id)
        val inv = createDirectInvitation(owner.id, group.id, invited.id)

        mockMvc.perform(
            post("/api/v1/invitations/${inv.id}/reject")
                .header("Authorization", "Bearer ${token("other@test.com")}")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST reject returns 404 for unknown invitation`() {
        generator.user(email = "user@test.com")

        mockMvc.perform(
            post("/api/v1/invitations/999/reject")
                .header("Authorization", "Bearer ${token("user@test.com")}")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `POST reject returns 401 without token`() {
        mockMvc.perform(post("/api/v1/invitations/1/reject"))
            .andExpect(status().isUnauthorized)
    }
}
