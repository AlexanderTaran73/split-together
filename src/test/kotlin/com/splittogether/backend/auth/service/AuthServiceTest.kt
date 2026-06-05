package com.splittogether.backend.auth.service

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.*
import com.splittogether.backend.auth.entity.EmailVerification
import com.splittogether.backend.auth.entity.RefreshToken
import com.splittogether.backend.auth.repository.EmailVerificationRepository
import com.splittogether.backend.auth.repository.RefreshTokenRepository
import com.splittogether.backend.common.exception.*
import com.splittogether.backend.common.repository.EmailVerificationPurposeRepository
import com.splittogether.backend.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var jwtService: JwtService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired private lateinit var emailVerificationRepository: EmailVerificationRepository
    @Autowired private lateinit var emailVerificationPurposeRepository: EmailVerificationPurposeRepository

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    fun `register creates an unverified user and saves a verification code`() {
        authService.register(RegisterRequest("user@test.com", "Password1!", "User"))

        val user = userRepository.findByEmail("user@test.com")
        assertNotNull(user)
        assertFalse(user!!.emailVerifiedAt != null)
        assertNotNull(emailVerificationRepository.findLatestUnused(user, "REGISTRATION"))
    }

    @Test
    fun `register throws EmailAlreadyExistsException for duplicate email`() {
        authService.register(RegisterRequest("user@test.com", "Password1!", "User"))

        assertFailsWith<EmailAlreadyExistsException> {
            authService.register(RegisterRequest("user@test.com", "Password2!", "Another"))
        }
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    fun `login throws EmailNotVerifiedException when email is not verified`() {
        generator.user(email = "user@test.com", emailVerified = false)

        assertFailsWith<EmailNotVerifiedException> {
            authService.login(LoginRequest("user@test.com", generator.defaultPassword))
        }
    }

    @Test
    fun `login throws InvalidCredentialsException for wrong password`() {
        generator.user(email = "user@test.com")

        assertFailsWith<InvalidCredentialsException> {
            authService.login(LoginRequest("user@test.com", "WrongPassword"))
        }
    }

    @Test
    fun `login throws InvalidCredentialsException for unknown email`() {
        assertFailsWith<InvalidCredentialsException> {
            authService.login(LoginRequest("nobody@test.com", "Password1!"))
        }
    }

    @Test
    fun `login returns valid tokens for correct credentials and verified email`() {
        generator.user(email = "user@test.com")

        val response = authService.login(LoginRequest("user@test.com", generator.defaultPassword))

        assertNotNull(response.accessToken)
        assertNotNull(response.refreshToken)
        assertTrue(jwtService.isValid(response.accessToken))
    }

    // ── verifyEmail ───────────────────────────────────────────────────────────

    @Test
    fun `verifyEmail sets emailVerified to true for the correct code`() {
        authService.register(RegisterRequest("user@test.com", "Password1!", "User"))
        verifyUserEmail("user@test.com")

        assertTrue(userRepository.findByEmail("user@test.com")!!.emailVerifiedAt != null)
    }

    @Test
    fun `verifyEmail throws InvalidVerificationCodeException for wrong code`() {
        generator.user(email = "user@test.com", emailVerified = false)

        assertFailsWith<InvalidVerificationCodeException> {
            authService.verifyEmail(VerifyEmailRequest("user@test.com", "999999"))
        }
    }

    @Test
    fun `verifyEmail throws InvalidVerificationCodeException for expired code`() {
        val user = generator.user(email = "user@test.com", emailVerified = false)
        val purpose = emailVerificationPurposeRepository.findByCode("REGISTRATION")!!
        emailVerificationRepository.save(
            EmailVerification(
                user = user,
                code = "999999",
                purpose = purpose,
                expiresAt = Instant.now().minus(1, ChronoUnit.HOURS)
            )
        )

        assertFailsWith<InvalidVerificationCodeException> {
            authService.verifyEmail(VerifyEmailRequest("user@test.com", "999999"))
        }
    }

    // ── resendVerification ────────────────────────────────────────────────────

    @Test
    fun `resendVerification saves a new verification code for unverified user`() {
        generator.user(email = "user@test.com", emailVerified = false)
        val countBefore = emailVerificationRepository.count()

        authService.resendVerification(ResendVerificationRequest("user@test.com"))

        assertTrue(emailVerificationRepository.count() > countBefore)
    }

    @Test
    fun `resendVerification throws EmailAlreadyVerifiedException when already verified`() {
        generator.user(email = "user@test.com")

        assertFailsWith<EmailAlreadyVerifiedException> {
            authService.resendVerification(ResendVerificationRequest("user@test.com"))
        }
    }

    @Test
    fun `resendVerification throws UserNotFoundException for unknown email`() {
        assertFailsWith<UserNotFoundException> {
            authService.resendVerification(ResendVerificationRequest("nobody@test.com"))
        }
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    fun `refresh returns new valid tokens for a valid refresh token`() {
        generator.user(email = "user@test.com")
        val tokens = authService.login(LoginRequest("user@test.com", generator.defaultPassword))

        val newTokens = authService.refresh(RefreshRequest(tokens.refreshToken))

        assertNotNull(newTokens.accessToken)
        assertNotNull(newTokens.refreshToken)
        assertTrue(jwtService.isValid(newTokens.accessToken))
    }

    @Test
    fun `refresh invalidates the used token so it cannot be reused`() {
        generator.user(email = "user@test.com")
        val tokens = authService.login(LoginRequest("user@test.com", generator.defaultPassword))
        authService.refresh(RefreshRequest(tokens.refreshToken))

        assertFailsWith<InvalidTokenException> {
            authService.refresh(RefreshRequest(tokens.refreshToken))
        }
    }

    @Test
    fun `refresh throws InvalidTokenException for completely invalid token`() {
        assertFailsWith<InvalidTokenException> {
            authService.refresh(RefreshRequest("totally-invalid-token"))
        }
    }

    @Test
    fun `refresh throws InvalidTokenException for expired token`() {
        val user = generator.user(email = "user@test.com")
        val rawToken = "expired-raw-token"
        refreshTokenRepository.save(
            RefreshToken(
                user = user,
                tokenHash = hashToken(rawToken),
                expiresAt = Instant.now().minus(1, ChronoUnit.HOURS)
            )
        )

        assertFailsWith<InvalidTokenException> {
            authService.refresh(RefreshRequest(rawToken))
        }
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    fun `logout revokes all active tokens so refresh no longer works`() {
        val user = generator.user(email = "user@test.com")
        val tokens = authService.login(LoginRequest("user@test.com", generator.defaultPassword))

        authService.logout(user.id)

        assertFailsWith<InvalidTokenException> {
            authService.refresh(RefreshRequest(tokens.refreshToken))
        }
    }

    // ── requestEmailChange ────────────────────────────────────────────────────

    @Test
    fun `requestEmailChange saves EMAIL_CHANGE verification for new email`() {
        val user = generator.user(email = "user@test.com")

        authService.requestEmailChange(user.id, "new@test.com")

        val verification = emailVerificationRepository.findLatestUnused(user, "EMAIL_CHANGE")
        assertNotNull(verification)
        kotlin.test.assertEquals("new@test.com", verification!!.newEmail)
    }

    @Test
    fun `requestEmailChange throws EmailAlreadyExistsException when new email is taken`() {
        val user = generator.user(email = "user@test.com")
        generator.user(email = "other@test.com")

        assertFailsWith<EmailAlreadyExistsException> {
            authService.requestEmailChange(user.id, "other@test.com")
        }
    }

    // ── confirmEmailChange ────────────────────────────────────────────────────

    @Test
    fun `confirmEmailChange updates user email to new email`() {
        val user = generator.user(email = "user@test.com")
        authService.requestEmailChange(user.id, "new@test.com")
        val code = emailVerificationRepository.findLatestUnused(user, "EMAIL_CHANGE")!!.code

        authService.confirmEmailChange(user.id, code)

        kotlin.test.assertEquals("new@test.com", userRepository.findById(user.id).get().email)
    }

    @Test
    fun `confirmEmailChange throws InvalidVerificationCodeException for wrong code`() {
        val user = generator.user(email = "user@test.com")
        authService.requestEmailChange(user.id, "new@test.com")

        assertFailsWith<InvalidVerificationCodeException> {
            authService.confirmEmailChange(user.id, "999999")
        }
    }

    @Test
    fun `confirmEmailChange throws InvalidVerificationCodeException for expired code`() {
        val user = generator.user(email = "user@test.com")
        val purpose = emailVerificationPurposeRepository.findByCode("EMAIL_CHANGE")!!
        emailVerificationRepository.save(
            EmailVerification(
                user = user,
                code = "111111",
                purpose = purpose,
                newEmail = "new@test.com",
                expiresAt = Instant.now().minus(1, ChronoUnit.HOURS)
            )
        )

        assertFailsWith<InvalidVerificationCodeException> {
            authService.confirmEmailChange(user.id, "111111")
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun verifyUserEmail(email: String) {
        val user = userRepository.findByEmail(email)!!
        val verification = emailVerificationRepository.findLatestUnused(user, "REGISTRATION")!!
        authService.verifyEmail(VerifyEmailRequest(email, verification.code))
    }

    private fun hashToken(token: String): String =
        Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        )
}
