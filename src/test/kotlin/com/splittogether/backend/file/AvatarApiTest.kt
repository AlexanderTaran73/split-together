package com.splittogether.backend.file

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.LoginRequest
import com.splittogether.backend.auth.service.AuthService
import com.splittogether.backend.group.dto.CreateGroupRequest
import com.splittogether.backend.group.dto.CreateInvitationRequest
import com.splittogether.backend.group.dto.JoinGroupRequest
import com.splittogether.backend.group.service.GroupService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AvatarApiTest : AbstractIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var groupService: GroupService

    private fun token(email: String): String =
        authService.login(LoginRequest(email, generator.defaultPassword)).accessToken

    private fun image(bytes: ByteArray = byteArrayOf(1, 2, 3)) =
        MockMultipartFile("file", "avatar.png", "image/png", bytes)

    // ── user avatar ───────────────────────────────────────────────────────────

    @Test
    fun `PUT user avatar returns 200 with presigned url and persists`() {
        generator.user(email = "u@test.com")

        mockMvc.perform(
            multipart("/api/v1/users/me/avatar").file(image())
                .with { it.method = "PUT"; it }
                .header("Authorization", "Bearer ${token("u@test.com")}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.avatarUrl").isNotEmpty)

        mockMvc.perform(
            get("/api/v1/users/me").header("Authorization", "Bearer ${token("u@test.com")}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.avatarUrl").isNotEmpty)
    }

    @Test
    fun `DELETE user avatar returns 204 and clears it`() {
        generator.user(email = "u@test.com")
        mockMvc.perform(
            multipart("/api/v1/users/me/avatar").file(image())
                .with { it.method = "PUT"; it }
                .header("Authorization", "Bearer ${token("u@test.com")}")
        ).andExpect(status().isOk)

        mockMvc.perform(
            delete("/api/v1/users/me/avatar").header("Authorization", "Bearer ${token("u@test.com")}")
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/v1/users/me").header("Authorization", "Bearer ${token("u@test.com")}")
        ).andExpect(jsonPath("$.avatarUrl").doesNotExist())
    }

    @Test
    fun `PUT user avatar with non-image returns 415`() {
        generator.user(email = "u@test.com")
        mockMvc.perform(
            multipart("/api/v1/users/me/avatar")
                .file(MockMultipartFile("file", "note.txt", "text/plain", byteArrayOf(1)))
                .with { it.method = "PUT"; it }
                .header("Authorization", "Bearer ${token("u@test.com")}")
        ).andExpect(status().isUnsupportedMediaType)
    }

    @Test
    fun `PUT user avatar over size limit returns 413`() {
        generator.user(email = "u@test.com")
        val tooLarge = ByteArray(5 * 1024 * 1024 + 1)
        mockMvc.perform(
            multipart("/api/v1/users/me/avatar").file(image(tooLarge))
                .with { it.method = "PUT"; it }
                .header("Authorization", "Bearer ${token("u@test.com")}")
        ).andExpect(status().isPayloadTooLarge())
    }

    @Test
    fun `PUT user avatar without token returns 401`() {
        mockMvc.perform(
            multipart("/api/v1/users/me/avatar").file(image()).with { it.method = "PUT"; it }
        ).andExpect(status().isUnauthorized)
    }

    // ── group avatar ──────────────────────────────────────────────────────────

    @Test
    fun `PUT group avatar by owner returns 200 with presigned url`() {
        val owner = generator.user(email = "owner@test.com")
        val group = groupService.createGroup(owner.id, CreateGroupRequest("G", null, "RUB"))

        mockMvc.perform(
            multipart("/api/v1/groups/${group.id}/avatar").file(image())
                .with { it.method = "PUT"; it }
                .header("Authorization", "Bearer ${token("owner@test.com")}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.avatarUrl").isNotEmpty)
    }

    @Test
    fun `PUT group avatar by plain member returns 403`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = groupService.createGroup(owner.id, CreateGroupRequest("G", null, "RUB"))
        val invite = groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("LINK"))
        groupService.joinGroup(member.id, JoinGroupRequest(invite.token!!))

        mockMvc.perform(
            multipart("/api/v1/groups/${group.id}/avatar").file(image())
                .with { it.method = "PUT"; it }
                .header("Authorization", "Bearer ${token("member@test.com")}")
        ).andExpect(status().isForbidden)
    }
}
