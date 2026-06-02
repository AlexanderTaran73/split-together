package com.splittogether.backend.auth.service

import com.splittogether.backend.auth.dto.*
import com.splittogether.backend.auth.entity.EmailVerification
import com.splittogether.backend.auth.entity.RefreshToken
import com.splittogether.backend.auth.repository.EmailVerificationRepository
import com.splittogether.backend.auth.repository.RefreshTokenRepository
import com.splittogether.backend.common.entity.EmailVerificationPurpose
import com.splittogether.backend.common.entity.PlatformRole
import com.splittogether.backend.common.exception.*
import com.splittogether.backend.common.repository.EmailVerificationPurposeRepository
import com.splittogether.backend.common.repository.PlatformRoleRepository
import com.splittogether.backend.email.service.EmailService
import com.splittogether.backend.user.entity.User
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailVerificationRepository: EmailVerificationRepository,
    private val platformRoleRepository: PlatformRoleRepository,
    private val emailVerificationPurposeRepository: EmailVerificationPurposeRepository,
    private val jwtService: JwtService,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun register(request: RegisterRequest) {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException("Email already in use")
        }
        val userRole = platformRoleRepository.findByCode(PlatformRole.USER)
            ?: error("Reference data missing: USER role")

        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)!!,
            displayName = request.displayName
        )
        user.platformRoles.add(userRole)
        userRepository.save(user)

        sendVerificationCode(user, EmailVerificationPurpose.REGISTRATION)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException("Invalid email or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid email or password")
        }
        if (!user.emailVerified) {
            throw EmailNotVerifiedException("Email not verified")
        }
        return issueTokens(user)
    }

    @Transactional
    fun refresh(request: RefreshRequest): AuthResponse {
        val tokenHash = hashToken(request.refreshToken)
        val token = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
            ?: throw InvalidTokenException("Invalid or expired refresh token")

        if (token.expiresAt.isBefore(Instant.now())) {
            throw InvalidTokenException("Refresh token expired")
        }
        token.revokedAt = Instant.now()
        refreshTokenRepository.save(token)

        return issueTokens(token.user)
    }

    @Transactional
    fun logout(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }
        refreshTokenRepository.revokeAllActiveByUser(user, Instant.now())
    }

    @Transactional
    fun verifyEmail(request: VerifyEmailRequest) {
        val user = userRepository.findByEmail(request.email)
            ?: throw UserNotFoundException("User not found")

        val verification = emailVerificationRepository.findLatestUnused(user, EmailVerificationPurpose.REGISTRATION)
            ?: throw InvalidVerificationCodeException("No pending verification found")

        if (verification.expiresAt.isBefore(Instant.now())) {
            throw InvalidVerificationCodeException("Verification code expired")
        }
        if (verification.code != request.code) {
            throw InvalidVerificationCodeException("Invalid verification code")
        }
        verification.usedAt = Instant.now()
        user.emailVerified = true
    }

    @Transactional
    fun resendVerification(request: ResendVerificationRequest) {
        val user = userRepository.findByEmail(request.email)
            ?: throw UserNotFoundException("User not found")

        if (user.emailVerified) {
            throw EmailAlreadyVerifiedException("Email is already verified")
        }
        sendVerificationCode(user, EmailVerificationPurpose.REGISTRATION)
    }

    private fun issueTokens(user: User): AuthResponse {
        val accessToken = jwtService.generateAccessToken(user)
        val rawToken = UUID.randomUUID().toString()
        refreshTokenRepository.save(
            RefreshToken(
                user = user,
                tokenHash = hashToken(rawToken),
                expiresAt = Instant.now().plus(30, ChronoUnit.DAYS)
            )
        )
        return AuthResponse(accessToken = accessToken, refreshToken = rawToken)
    }

    private fun sendVerificationCode(user: User, purposeCode: String) {
        val purpose = emailVerificationPurposeRepository.findByCode(purposeCode)
            ?: error("Reference data missing: purpose $purposeCode")

        val code = (100000..999999).random().toString()
        emailVerificationRepository.save(
            EmailVerification(
                user = user,
                code = code,
                purpose = purpose,
                expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES)
            )
        )
        emailService.sendVerificationCode(user.email, code)
    }

    private fun hashToken(token: String): String =
        Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        )
}
