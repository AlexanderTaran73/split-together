package com.splittogether.backend.user.controller

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.LoginRequest
import com.splittogether.backend.auth.dto.RegisterRequest
import com.splittogether.backend.auth.dto.VerifyEmailRequest
import com.splittogether.backend.auth.repository.EmailVerificationRepository
import com.splittogether.backend.auth.service.AuthService
import com.splittogether.backend.user.repository.UserRepository
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
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var emailVerificationRepository: EmailVerificationRepository

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun registerAndVerify(email: String = "user@test.com", password: String = "Password1!") {
        authService.register(RegisterRequest(email, password, "User"))
        val user = userRepository.findByEmail(email)!!
        val code = emailVerificationRepository.findLatestUnused(user, "REGISTRATION")!!.code
        authService.verifyEmail(VerifyEmailRequest(email, code))
    }

    private fun loginAndGetToken(email: String = "user@test.com", password: String = "Password1!"): String =
        authService.login(LoginRequest(email, password)).accessToken

    // ── GET /me ───────────────────────────────────────────────────────────────

    @Test
    fun `GET me returns 200 with user profile`() {
        registerAndVerify()
        val token = loginAndGetToken()

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
        registerAndVerify()
        val token = loginAndGetToken()

        mockMvc.perform(
            get("/api/v1/users")
                .header("Authorization", "Bearer $token")
                .param("query", "user")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].email").value("user@test.com"))
    }

    @Test
    fun `GET search returns 400 when query is shorter than 2 characters`() {
        registerAndVerify()
        val token = loginAndGetToken()

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
        registerAndVerify()
        val token = loginAndGetToken()

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
        registerAndVerify()
        val token = loginAndGetToken()

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
}
