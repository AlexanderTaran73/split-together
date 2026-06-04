package com.splittogether.backend.auth.service

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.RegisterRequest
import com.splittogether.backend.email.service.EmailService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VerificationEmailTest : AbstractIntegrationTest() {

    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var emailService: EmailService

    @Test
    fun `registration sends exactly one email`() {
        authService.register(RegisterRequest("user@test.com", "Password1!", "User"))
        assertEquals(1, capturingMailSender.messages.size)
    }

    @Test
    fun `registration email is sent to registered address`() {
        authService.register(RegisterRequest("user@test.com", "Password1!", "User"))
        assertEquals("user@test.com", capturingMailSender.last().allRecipients[0].toString())
    }

    @Test
    fun `requestEmailChange sends email to new address`() {
        val user = generator.user(email = "user@test.com")
        authService.requestEmailChange(user.id, "new@test.com")
        assertEquals("new@test.com", capturingMailSender.last().allRecipients[0].toString())
    }

    @Test
    fun `verification-code email matches expected snapshot`() {
        val actual = emailService.renderTemplate("verification-code", mapOf("code" to "000000"))
        val expected = ClassPathResource("expected-verification-code.html")
            .inputStream.bufferedReader(Charsets.UTF_8).readText()

        assertEquals(normalize(expected), normalize(actual))
    }

    private fun normalize(html: String) =
        html.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
}
