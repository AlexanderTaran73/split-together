package com.splittogether.backend.auth.controller

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.*
import com.splittogether.backend.auth.repository.EmailVerificationRepository
import com.splittogether.backend.auth.repository.RefreshTokenRepository
import com.splittogether.backend.auth.service.AuthService
import com.splittogether.backend.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthControllerTest : AbstractIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired private lateinit var emailVerificationRepository: EmailVerificationRepository

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun registerAndVerify(email: String = "user@test.com", password: String = "Password1!") {
        authService.register(RegisterRequest(email, password, "User"))
        val user = userRepository.findByEmail(email)!!
        val code = emailVerificationRepository.findLatestUnused(user, "REGISTRATION")!!.code
        authService.verifyEmail(VerifyEmailRequest(email, code))
    }

    private fun loginAndGetAccessToken(email: String = "user@test.com", password: String = "Password1!"): String =
        authService.login(LoginRequest(email, password)).accessToken

    // ── POST /register ────────────────────────────────────────────────────────

    @Test
    fun `register returns 201 for valid request`() {
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@test.com","password":"Password1!","displayName":"User"}""")
        ).andExpect(status().isCreated)
    }

    @Test
    fun `register returns 400 for invalid email format`() {
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"not-an-email","password":"Password1!","displayName":"User"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `register returns 400 for password shorter than 8 characters`() {
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@test.com","password":"short","displayName":"User"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `register returns 400 for blank displayName`() {
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@test.com","password":"Password1!","displayName":""}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `register returns 409 for duplicate email`() {
        authService.register(RegisterRequest("user@test.com", "Password1!", "User"))

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@test.com","password":"Password1!","displayName":"User"}""")
        ).andExpect(status().isConflict)
    }

    // ── POST /login ───────────────────────────────────────────────────────────

    @Test
    fun `login returns 200 with tokens for valid verified credentials`() {
        registerAndVerify()

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@test.com","password":"Password1!"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
    }

    @Test
    fun `login returns 401 for wrong password`() {
        registerAndVerify()

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@test.com","password":"WrongPassword"}""")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `login returns 403 when email is not verified`() {
        authService.register(RegisterRequest("user@test.com", "Password1!", "User"))

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@test.com","password":"Password1!"}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `login returns 400 for invalid request body`() {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"not-email","password":""}""")
        ).andExpect(status().isBadRequest)
    }

    // ── POST /verify-email ────────────────────────────────────────────────────

    @Test
    fun `verify-email returns 200 for correct code`() {
        authService.register(RegisterRequest("user@test.com", "Password1!", "User"))
        val user = userRepository.findByEmail("user@test.com")!!
        val code = emailVerificationRepository.findLatestUnused(user, "REGISTRATION")!!.code

        mockMvc.perform(
            post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@test.com","code":"$code"}""")
        ).andExpect(status().isOk)
    }

    @Test
    fun `verify-email returns 400 for wrong code`() {
        authService.register(RegisterRequest("user@test.com", "Password1!", "User"))

        mockMvc.perform(
            post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@test.com","code":"000000"}""")
        ).andExpect(status().isBadRequest)
    }

    // ── POST /resend-verification ─────────────────────────────────────────────

    @Test
    fun `resend-verification returns 200 for unverified user`() {
        authService.register(RegisterRequest("user@test.com", "Password1!", "User"))

        mockMvc.perform(
            post("/api/v1/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@test.com"}""")
        ).andExpect(status().isOk)
    }

    @Test
    fun `resend-verification returns 409 for already verified user`() {
        registerAndVerify()

        mockMvc.perform(
            post("/api/v1/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@test.com"}""")
        ).andExpect(status().isConflict)
    }

    // ── POST /refresh ─────────────────────────────────────────────────────────

    @Test
    fun `refresh returns 200 with new tokens for valid refresh token`() {
        registerAndVerify()
        val tokens = authService.login(LoginRequest("user@test.com", "Password1!"))

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"${tokens.refreshToken}"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
    }

    @Test
    fun `refresh returns 401 for invalid token`() {
        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"invalid-token"}""")
        ).andExpect(status().isUnauthorized)
    }

    // ── POST /logout ──────────────────────────────────────────────────────────

    @Test
    fun `logout returns 204 for authenticated user`() {
        registerAndVerify()
        val accessToken = loginAndGetAccessToken()

        mockMvc.perform(
            post("/api/v1/auth/logout")
                .header("Authorization", "Bearer $accessToken")
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `logout returns 401 without Authorization header`() {
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isUnauthorized)
    }
}
