package com.splittogether.backend.user.controller

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.LoginRequest
import com.splittogether.backend.auth.service.AuthService
import com.splittogether.backend.group.dto.CreateGroupRequest
import com.splittogether.backend.group.dto.CreateInvitationRequest
import com.splittogether.backend.group.entity.Group
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.group.service.GroupService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserControllerTest : AbstractIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var groupRepository: GroupRepository

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun token(email: String = "user@test.com"): String =
        authService.login(LoginRequest(email, generator.defaultPassword)).accessToken

    private fun createGroup(userId: Long): Group {
        groupService.createGroup(userId, CreateGroupRequest("Test Group", null, "RUB"))
        return groupRepository.findAll().last()
    }

    // ── GET /me ───────────────────────────────────────────────────────────────

    @Test
    fun `GET me returns 200 with user profile`() {
        generator.user(email = "user@test.com")
        val token = token()

        mockMvc.perform(
            get("/api/v1/users/me")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.email").value("user@test.com"))
            .andExpect(jsonPath("$.displayName").exists())
    }

    @Test
    fun `GET me returns 401 without token`() {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized)
    }

    // ── GET /?query= ──────────────────────────────────────────────────────────

    @Test
    fun `GET search returns 200 with matching users`() {
        generator.user(email = "user@test.com", displayName = "User")
        generator.user(email = "target@test.com", displayName = "Target")
        val token = token()

        mockMvc.perform(
            get("/api/v1/users")
                .header("Authorization", "Bearer $token")
                .param("query", "target")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].email").value("target@test.com"))
    }

    @Test
    fun `GET search returns 400 when query is shorter than 2 characters`() {
        generator.user(email = "user@test.com")
        val token = token()

        mockMvc.perform(
            get("/api/v1/users")
                .header("Authorization", "Bearer $token")
                .param("query", "a")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET search returns 401 without token`() {
        mockMvc.perform(
            get("/api/v1/users").param("query", "user")
        ).andExpect(status().isUnauthorized)
    }

    // ── PUT /me ───────────────────────────────────────────────────────────────

    @Test
    fun `PUT me returns 200 with updated displayName`() {
        generator.user(email = "user@test.com")
        val token = token()

        mockMvc.perform(
            put("/api/v1/users/me")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"Updated Name"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("Updated Name"))
    }

    @Test
    fun `PUT me returns 400 for blank displayName`() {
        generator.user(email = "user@test.com")
        val token = token()

        mockMvc.perform(
            put("/api/v1/users/me")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":""}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT me returns 401 without token`() {
        mockMvc.perform(
            put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"Name"}""")
        ).andExpect(status().isUnauthorized)
    }

    // ── GET / PUT /me/privacy ─────────────────────────────────────────────────

    @Test
    fun `GET me privacy returns 200 with default settings`() {
        generator.user(email = "user@test.com")

        mockMvc.perform(
            get("/api/v1/users/me/privacy")
                .header("Authorization", "Bearer ${token()}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.searchVisibility").value("EVERYONE"))
            .andExpect(jsonPath("$.groupInvitePolicy").value("ANYONE"))
    }

    @Test
    fun `PUT me privacy returns 200 with updated settings`() {
        generator.user(email = "user@test.com")

        mockMvc.perform(
            put("/api/v1/users/me/privacy")
                .header("Authorization", "Bearer ${token()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"searchVisibility":"NOBODY","groupInvitePolicy":"FRIENDS"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.searchVisibility").value("NOBODY"))
            .andExpect(jsonPath("$.groupInvitePolicy").value("FRIENDS"))
    }

    @Test
    fun `PUT me privacy returns 400 for unknown code`() {
        generator.user(email = "user@test.com")

        mockMvc.perform(
            put("/api/v1/users/me/privacy")
                .header("Authorization", "Bearer ${token()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"searchVisibility":"BOGUS","groupInvitePolicy":"ANYONE"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET me privacy returns 401 without token`() {
        mockMvc.perform(get("/api/v1/users/me/privacy"))
            .andExpect(status().isUnauthorized)
    }

    // ── GET /me/invitations ───────────────────────────────────────────────────

    @Test
    fun `GET me invitations returns 200 with pending direct invitations`() {
        val owner = generator.user(email = "owner@test.com")
        val invited = generator.user(email = "user@test.com")
        val group = createGroup(owner.id)
        groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("DIRECT", invitedUserId = invited.id))

        mockMvc.perform(
            get("/api/v1/users/me/invitations")
                .header("Authorization", "Bearer ${token()}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].groupId").value(group.id))
            .andExpect(jsonPath("$[0].invitedById").value(owner.id))
    }

    @Test
    fun `GET me invitations returns 200 with empty list when no invitations`() {
        generator.user(email = "user@test.com")

        mockMvc.perform(
            get("/api/v1/users/me/invitations")
                .header("Authorization", "Bearer ${token()}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `GET me invitations returns 401 without token`() {
        mockMvc.perform(get("/api/v1/users/me/invitations"))
            .andExpect(status().isUnauthorized)
    }
}
