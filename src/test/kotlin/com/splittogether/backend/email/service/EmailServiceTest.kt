package com.splittogether.backend.email.service

import com.splittogether.backend.email.CapturingMailSender
import com.splittogether.backend.email.config.EmailProperties
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailServiceTest {

    private val capturingMailSender = CapturingMailSender()
    private val emailService = EmailService(
        capturingMailSender,
        EmailProperties(from = "noreply@splittogether.com")
    )

    // ── layout ────────────────────────────────────────────────────────────────

    @Test
    fun `rendered email contains layout structure`() {
        val html = emailService.renderTemplate("verification-code", mapOf("code" to "123456"))
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("SplitTogether"))
        assertTrue(html.contains("</html>"))
    }

    @Test
    fun `layout placeholder is replaced with content`() {
        val html = emailService.renderTemplate("verification-code", mapOf("code" to "123456"))
        assertFalse(html.contains("{{content}}"))
    }

    // ── content ───────────────────────────────────────────────────────────────

    @Test
    fun `rendered email contains verification code`() {
        val html = emailService.renderTemplate("verification-code", mapOf("code" to "123456"))
        assertTrue(html.contains("123456"))
    }

    @Test
    fun `rendered email leaves no unresolved placeholders`() {
        val html = emailService.renderTemplate("verification-code", mapOf("code" to "123456"))
        assertFalse(html.contains("{{"))
    }

    // ── send ──────────────────────────────────────────────────────────────────

    @Test
    fun `send delivers to correct recipient`() {
        emailService.send {
            to("user@test.com")
            subject("Verification Code — SplitTogether")
            template("verification-code")
            variable("code", "123456")
        }
        assertEquals("user@test.com", capturingMailSender.last().allRecipients[0].toString())
    }

    @Test
    fun `send sets correct subject`() {
        emailService.send {
            to("user@test.com")
            subject("Verification Code — SplitTogether")
            template("verification-code")
            variable("code", "123456")
        }
        assertEquals("Verification Code — SplitTogether", capturingMailSender.last().subject)
    }

    @Test
    fun `send sets correct from address`() {
        emailService.send {
            to("user@test.com")
            subject("Verification Code — SplitTogether")
            template("verification-code")
            variable("code", "123456")
        }
        assertEquals("noreply@splittogether.com", capturingMailSender.last().from[0].toString())
    }
}
