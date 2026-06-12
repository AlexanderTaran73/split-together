package com.splittogether.backend.dictionary.controller

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.LoginRequest
import com.splittogether.backend.auth.service.AuthService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class DictionaryControllerTest : AbstractIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var authService: AuthService

    private fun token(): String =
        authService.login(LoginRequest(generator.user().email, generator.defaultPassword)).accessToken

    // ── GET /currencies ───────────────────────────────────────────────────────

    @Test
    fun `GET currencies returns 200 with all currencies`() {
        mockMvc.perform(
            get("/api/v1/dictionaries/currencies")
                .header("Authorization", "Bearer ${token()}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.code == 'RUB')]").exists())
            .andExpect(jsonPath("$[?(@.code == 'USD')]").exists())
            .andExpect(jsonPath("$[?(@.code == 'EUR')]").exists())
    }

    @Test
    fun `GET currencies returns 401 without token`() {
        mockMvc.perform(get("/api/v1/dictionaries/currencies"))
            .andExpect(status().isUnauthorized)
    }

    // ── GET /expense-categories ───────────────────────────────────────────────

    @Test
    fun `GET expense-categories returns 200 with all categories`() {
        mockMvc.perform(
            get("/api/v1/dictionaries/expense-categories")
                .header("Authorization", "Bearer ${token()}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.code == 'FOOD')]").exists())
            .andExpect(jsonPath("$[?(@.code == 'TRANSPORT')]").exists())
            .andExpect(jsonPath("$[?(@.code == 'OTHER')]").exists())
    }

    @Test
    fun `GET expense-categories returns 401 without token`() {
        mockMvc.perform(get("/api/v1/dictionaries/expense-categories"))
            .andExpect(status().isUnauthorized)
    }

    // ── GET /split-methods ────────────────────────────────────────────────────

    @Test
    fun `GET split-methods returns 200 with all methods`() {
        mockMvc.perform(
            get("/api/v1/dictionaries/split-methods")
                .header("Authorization", "Bearer ${token()}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.code == 'EQUAL')]").exists())
            .andExpect(jsonPath("$[?(@.code == 'SHARES')]").exists())
            .andExpect(jsonPath("$[?(@.code == 'EXACT')]").exists())
    }

    @Test
    fun `GET split-methods returns 401 without token`() {
        mockMvc.perform(get("/api/v1/dictionaries/split-methods"))
            .andExpect(status().isUnauthorized)
    }

    // ── GET /friendship-statuses ──────────────────────────────────────────────

    @Test
    fun `GET friendship-statuses returns 200 with all statuses`() {
        mockMvc.perform(
            get("/api/v1/dictionaries/friendship-statuses")
                .header("Authorization", "Bearer ${token()}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.code == 'PENDING')]").exists())
            .andExpect(jsonPath("$[?(@.code == 'ACCEPTED')]").exists())
            .andExpect(jsonPath("$[?(@.code == 'BLOCKED')]").exists())
    }

    @Test
    fun `GET friendship-statuses returns 401 without token`() {
        mockMvc.perform(get("/api/v1/dictionaries/friendship-statuses"))
            .andExpect(status().isUnauthorized)
    }

    // ── GET /search-visibilities ──────────────────────────────────────────────

    @Test
    fun `GET search-visibilities returns 200 with all options`() {
        mockMvc.perform(
            get("/api/v1/dictionaries/search-visibilities")
                .header("Authorization", "Bearer ${token()}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.code == 'EVERYONE')]").exists())
            .andExpect(jsonPath("$[?(@.code == 'FRIENDS')]").exists())
            .andExpect(jsonPath("$[?(@.code == 'NOBODY')]").exists())
    }

    // ── GET /group-invite-policies ────────────────────────────────────────────

    @Test
    fun `GET group-invite-policies returns 200 with all options`() {
        mockMvc.perform(
            get("/api/v1/dictionaries/group-invite-policies")
                .header("Authorization", "Bearer ${token()}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.code == 'ANYONE')]").exists())
            .andExpect(jsonPath("$[?(@.code == 'FRIENDS')]").exists())
            .andExpect(jsonPath("$[?(@.code == 'INVITE_ONLY')]").exists())
    }

    @Test
    fun `GET group-invite-policies returns 401 without token`() {
        mockMvc.perform(get("/api/v1/dictionaries/group-invite-policies"))
            .andExpect(status().isUnauthorized)
    }
}
