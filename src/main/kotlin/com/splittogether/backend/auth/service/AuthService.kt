package com.splittogether.backend.auth.service

import com.splittogether.backend.auth.config.AuthProperties
import com.splittogether.backend.auth.dto.*
import com.splittogether.backend.auth.entity.EmailVerification
import com.splittogether.backend.auth.entity.RefreshToken
import com.splittogether.backend.auth.event.EmailChangeRequestedEvent
import com.splittogether.backend.auth.event.PasswordResetRequestedEvent
import com.splittogether.backend.auth.event.UserRegisteredEvent
import com.splittogether.backend.auth.repository.EmailVerificationRepository
import com.splittogether.backend.auth.repository.RefreshTokenRepository
import com.splittogether.backend.common.entity.EmailVerificationPurpose
import com.splittogether.backend.common.entity.PlatformRole
import com.splittogether.backend.common.exception.*
import com.splittogether.backend.common.repository.EmailVerificationPurposeRepository
import com.splittogether.backend.common.repository.PlatformRoleRepository
import com.splittogether.backend.user.entity.GroupInvitePolicy
import com.splittogether.backend.user.entity.SearchVisibility
import com.splittogether.backend.user.entity.User
import com.splittogether.backend.user.repository.GroupInvitePolicyRepository
import com.splittogether.backend.user.repository.SearchVisibilityRepository
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class AuthService(
    private val authProperties: AuthProperties,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailVerificationRepository: EmailVerificationRepository,
    private val platformRoleRepository: PlatformRoleRepository,
    private val emailVerificationPurposeRepository: EmailVerificationPurposeRepository,
    private val searchVisibilityRepository: SearchVisibilityRepository,
    private val groupInvitePolicyRepository: GroupInvitePolicyRepository,
    private val jwtService: JwtService,
    private val eventPublisher: ApplicationEventPublisher,
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
            displayName = request.displayName,
            searchVisibility = searchVisibilityRepository.findByCode(SearchVisibility.EVERYONE)
                ?: error("Reference data missing: search_visibility=EVERYONE"),
            groupInvitePolicy = groupInvitePolicyRepository.findByCode(GroupInvitePolicy.ANYONE)
                ?: error("Reference data missing: group_invite_policy=ANYONE")
        )
        user.platformRoles.add(userRole)
        userRepository.save(user)

        val code = saveVerificationCode(user, EmailVerificationPurpose.REGISTRATION)
        eventPublisher.publishEvent(UserRegisteredEvent(user.email, code))
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException("Invalid email or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid email or password")
        }
        if (user.emailVerifiedAt == null) {
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
        user.emailVerifiedAt = Instant.now()
    }

    @Transactional
    fun requestEmailChange(userId: Long, newEmail: String) {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }
        if (userRepository.existsByEmail(newEmail))
            throw EmailAlreadyExistsException("Email already in use")

        val code = saveVerificationCode(user, EmailVerificationPurpose.EMAIL_CHANGE, newEmail)
        eventPublisher.publishEvent(EmailChangeRequestedEvent(newEmail, code))
    }

    @Transactional
    fun confirmEmailChange(userId: Long, code: String) {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }

        val verification = emailVerificationRepository.findLatestUnused(user, EmailVerificationPurpose.EMAIL_CHANGE)
            ?: throw InvalidVerificationCodeException("No pending email change found")

        if (verification.expiresAt.isBefore(Instant.now()))
            throw InvalidVerificationCodeException("Verification code expired")
        if (verification.code != code)
            throw InvalidVerificationCodeException("Invalid verification code")

        verification.usedAt = Instant.now()
        user.email = verification.newEmail!!
    }

    @Transactional
    fun resendVerification(request: ResendVerificationRequest) {
        val user = userRepository.findByEmail(request.email)
            ?: throw UserNotFoundException("User not found")

        if (user.emailVerifiedAt != null) {
            throw EmailAlreadyVerifiedException("Email is already verified")
        }
        val code = saveVerificationCode(user, EmailVerificationPurpose.REGISTRATION)
        eventPublisher.publishEvent(UserRegisteredEvent(user.email, code))
    }

    @Transactional
    fun requestPasswordReset(request: PasswordResetRequest) {
        val user = userRepository.findByEmail(request.email) ?: return

        val code = saveVerificationCode(user, EmailVerificationPurpose.PASSWORD_RESET)
        eventPublisher.publishEvent(PasswordResetRequestedEvent(user.email, code))
    }

    @Transactional
    fun confirmPasswordReset(request: PasswordResetConfirmRequest) {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidVerificationCodeException("Invalid verification code")

        val verification = emailVerificationRepository.findLatestUnused(user, EmailVerificationPurpose.PASSWORD_RESET)
            ?: throw InvalidVerificationCodeException("No pending password reset found")

        if (verification.expiresAt.isBefore(Instant.now()))
            throw InvalidVerificationCodeException("Verification code expired")
        if (verification.code != request.code)
            throw InvalidVerificationCodeException("Invalid verification code")

        verification.usedAt = Instant.now()
        user.passwordHash = passwordEncoder.encode(request.newPassword)!!
        refreshTokenRepository.revokeAllActiveByUser(user, Instant.now())
    }

    private fun saveVerificationCode(user: User, purposeCode: String, newEmail: String? = null): String {
        val purpose = emailVerificationPurposeRepository.findByCode(purposeCode)
            ?: error("Reference data missing: purpose $purposeCode")
        val code = authProperties.verification.fixedCode.ifEmpty { (100000..999999).random().toString() }
        emailVerificationRepository.save(
            EmailVerification(
                user = user,
                code = code,
                purpose = purpose,
                newEmail = newEmail,
                expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES)
            )
        )
        return code
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

    private fun hashToken(token: String): String =
        Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        )
}
