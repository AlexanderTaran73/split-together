package com.splittogether.backend.auth.service

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.LoginRequest
import com.splittogether.backend.auth.dto.PasswordResetConfirmRequest
import com.splittogether.backend.auth.dto.PasswordResetRequest
import com.splittogether.backend.auth.dto.RefreshRequest
import com.splittogether.backend.auth.entity.EmailVerification
import com.splittogether.backend.auth.repository.EmailVerificationRepository
import com.splittogether.backend.common.exception.InvalidCredentialsException
import com.splittogether.backend.common.exception.InvalidTokenException
import com.splittogether.backend.common.exception.InvalidVerificationCodeException
import com.splittogether.backend.common.repository.EmailVerificationPurposeRepository
import com.splittogether.backend.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class PasswordResetTest : AbstractIntegrationTest() {

    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var emailVerificationRepository: EmailVerificationRepository
    @Autowired private lateinit var emailVerificationPurposeRepository: EmailVerificationPurposeRepository

    // ── requestPasswordReset ──────────────────────────────────────────────────

    @Test
    fun `requestPasswordReset saves a PASSWORD_RESET code for an existing user`() {
        val user = generator.user(email = "user@test.com")

        authService.requestPasswordReset(PasswordResetRequest("user@test.com"))

        assertNotNull(emailVerificationRepository.findLatestUnused(user, "PASSWORD_RESET"))
    }

    @Test
    fun `requestPasswordReset does nothing and does not throw for an unknown email`() {
        val countBefore = emailVerificationRepository.count()

        authService.requestPasswordReset(PasswordResetRequest("nobody@test.com"))

        kotlin.test.assertEquals(countBefore, emailVerificationRepository.count())
    }

    // ── confirmPasswordReset ──────────────────────────────────────────────────

    @Test
    fun `confirmPasswordReset changes the password so the new one works and the old one does not`() {
        generator.user(email = "user@test.com")
        authService.requestPasswordReset(PasswordResetRequest("user@test.com"))
        val code = currentCode("user@test.com")

        authService.confirmPasswordReset(PasswordResetConfirmRequest("user@test.com", code, "NewPassword1!"))

        assertNotNull(authService.login(LoginRequest("user@test.com", "NewPassword1!")).accessToken)
        assertFailsWith<InvalidCredentialsException> {
            authService.login(LoginRequest("user@test.com", generator.defaultPassword))
        }
    }

    @Test
    fun `confirmPasswordReset revokes all active refresh tokens`() {
        generator.user(email = "user@test.com")
        val tokens = authService.login(LoginRequest("user@test.com", generator.defaultPassword))
        authService.requestPasswordReset(PasswordResetRequest("user@test.com"))
        val code = currentCode("user@test.com")

        authService.confirmPasswordReset(PasswordResetConfirmRequest("user@test.com", code, "NewPassword1!"))

        assertFailsWith<InvalidTokenException> {
            authService.refresh(RefreshRequest(tokens.refreshToken))
        }
    }

    @Test
    fun `confirmPasswordReset marks the code as used so it cannot be reused`() {
        generator.user(email = "user@test.com")
        authService.requestPasswordReset(PasswordResetRequest("user@test.com"))
        val code = currentCode("user@test.com")
        authService.confirmPasswordReset(PasswordResetConfirmRequest("user@test.com", code, "NewPassword1!"))

        assertFailsWith<InvalidVerificationCodeException> {
            authService.confirmPasswordReset(PasswordResetConfirmRequest("user@test.com", code, "AnotherPass1!"))
        }
    }

    @Test
    fun `confirmPasswordReset throws InvalidVerificationCodeException for a wrong code`() {
        generator.user(email = "user@test.com")
        authService.requestPasswordReset(PasswordResetRequest("user@test.com"))

        assertFailsWith<InvalidVerificationCodeException> {
            authService.confirmPasswordReset(PasswordResetConfirmRequest("user@test.com", "999999", "NewPassword1!"))
        }
    }

    @Test
    fun `confirmPasswordReset throws InvalidVerificationCodeException for an expired code`() {
        val user = generator.user(email = "user@test.com")
        val purpose = emailVerificationPurposeRepository.findByCode("PASSWORD_RESET")!!
        emailVerificationRepository.save(
            EmailVerification(
                user = user,
                code = "123456",
                purpose = purpose,
                expiresAt = Instant.now().minus(1, ChronoUnit.HOURS)
            )
        )

        assertFailsWith<InvalidVerificationCodeException> {
            authService.confirmPasswordReset(PasswordResetConfirmRequest("user@test.com", "123456", "NewPassword1!"))
        }
    }

    @Test
    fun `confirmPasswordReset throws InvalidVerificationCodeException for an unknown email`() {
        assertFailsWith<InvalidVerificationCodeException> {
            authService.confirmPasswordReset(PasswordResetConfirmRequest("nobody@test.com", "123456", "NewPassword1!"))
        }
    }

    @Test
    fun `confirmPasswordReset throws InvalidVerificationCodeException when no reset was requested`() {
        generator.user(email = "user@test.com")

        assertFailsWith<InvalidVerificationCodeException> {
            authService.confirmPasswordReset(PasswordResetConfirmRequest("user@test.com", "123456", "NewPassword1!"))
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun currentCode(email: String): String {
        val user = userRepository.findByEmail(email)!!
        return emailVerificationRepository.findLatestUnused(user, "PASSWORD_RESET")!!.code
    }
}
