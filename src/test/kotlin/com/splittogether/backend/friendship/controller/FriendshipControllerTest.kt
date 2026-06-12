package com.splittogether.backend.friendship.controller

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.LoginRequest
import com.splittogether.backend.auth.service.AuthService
import com.splittogether.backend.friendship.service.FriendshipService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class FriendshipControllerTest : AbstractIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var friendshipService: FriendshipService

    private fun token(email: String): String =
        authService.login(LoginRequest(email, generator.defaultPassword)).accessToken

    // ── POST /friends/requests ────────────────────────────────────────────────

    @Test
    fun `POST request returns 201 and creates a pending request`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")

        mockMvc.perform(
            post("/api/v1/friends/requests")
                .header("Authorization", "Bearer ${token("a@test.com")}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":${b.id}}""")
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.userId").value(b.id))
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    @Test
    fun `POST request to self returns 400`() {
        val a = generator.user(email = "a@test.com")

        mockMvc.perform(
            post("/api/v1/friends/requests")
                .header("Authorization", "Bearer ${token("a@test.com")}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":${a.id}}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST request returns 401 without token`() {
        mockMvc.perform(
            post("/api/v1/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"userId":1}""")
        ).andExpect(status().isUnauthorized)
    }

    // ── accept / decline ──────────────────────────────────────────────────────

    @Test
    fun `POST accept returns 200 and links the friends`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        val req = friendshipService.sendRequest(a.id, b.id)

        mockMvc.perform(
            post("/api/v1/friends/requests/${req.id}/accept")
                .header("Authorization", "Bearer ${token("b@test.com")}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(a.id))
    }

    @Test
    fun `POST decline returns 204`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        val req = friendshipService.sendRequest(a.id, b.id)

        mockMvc.perform(
            post("/api/v1/friends/requests/${req.id}/decline")
                .header("Authorization", "Bearer ${token("b@test.com")}")
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `POST accept by another user returns 404`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        val other = generator.user(email = "other@test.com")
        val req = friendshipService.sendRequest(a.id, b.id)

        mockMvc.perform(
            post("/api/v1/friends/requests/${req.id}/accept")
                .header("Authorization", "Bearer ${token("other@test.com")}")
        ).andExpect(status().isNotFound)
    }

    // ── remove / block / unblock ──────────────────────────────────────────────

    @Test
    fun `DELETE friend returns 204`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        val req = friendshipService.sendRequest(a.id, b.id)
        friendshipService.acceptRequest(b.id, req.id)

        mockMvc.perform(
            delete("/api/v1/friends/${b.id}")
                .header("Authorization", "Bearer ${token("a@test.com")}")
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `POST block then DELETE block returns 204 each`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")

        mockMvc.perform(
            post("/api/v1/friends/${b.id}/block")
                .header("Authorization", "Bearer ${token("a@test.com")}")
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            delete("/api/v1/friends/${b.id}/block")
                .header("Authorization", "Bearer ${token("a@test.com")}")
        ).andExpect(status().isNoContent)
    }

    // ── listings ──────────────────────────────────────────────────────────────

    @Test
    fun `GET friends returns accepted friends`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        val req = friendshipService.sendRequest(a.id, b.id)
        friendshipService.acceptRequest(b.id, req.id)

        mockMvc.perform(
            get("/api/v1/friends")
                .header("Authorization", "Bearer ${token("a@test.com")}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userId").value(b.id))
    }

    @Test
    fun `GET incoming returns pending requests addressed to me`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        friendshipService.sendRequest(a.id, b.id)

        mockMvc.perform(
            get("/api/v1/friends/requests/incoming")
                .header("Authorization", "Bearer ${token("b@test.com")}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userId").value(a.id))
    }

    @Test
    fun `GET friends returns 401 without token`() {
        mockMvc.perform(get("/api/v1/friends"))
            .andExpect(status().isUnauthorized)
    }
}
