package com.splittogether.backend.auth.controller

import com.splittogether.backend.auth.dto.*
import com.splittogether.backend.auth.security.AppUserDetails
import com.splittogether.backend.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<Void> {
        authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(request))

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.refresh(request))

    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal user: AppUserDetails): ResponseEntity<Void> {
        authService.logout(user.userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/verify-email")
    fun verifyEmail(@Valid @RequestBody request: VerifyEmailRequest): ResponseEntity<Void> {
        authService.verifyEmail(request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/resend-verification")
    fun resendVerification(@Valid @RequestBody request: ResendVerificationRequest): ResponseEntity<Void> {
        authService.resendVerification(request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/change-email")
    fun changeEmail(
        @AuthenticationPrincipal user: AppUserDetails,
        @Valid @RequestBody request: ChangeEmailRequest
    ): ResponseEntity<Void> {
        authService.requestEmailChange(user.userId, request.newEmail)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/confirm-email-change")
    fun confirmEmailChange(
        @AuthenticationPrincipal user: AppUserDetails,
        @Valid @RequestBody request: ConfirmEmailChangeRequest
    ): ResponseEntity<Void> {
        authService.confirmEmailChange(user.userId, request.code)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/password-reset/request")
    fun requestPasswordReset(@Valid @RequestBody request: PasswordResetRequest): ResponseEntity<Void> {
        authService.requestPasswordReset(request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/password-reset/confirm")
    fun confirmPasswordReset(@Valid @RequestBody request: PasswordResetConfirmRequest): ResponseEntity<Void> {
        authService.confirmPasswordReset(request)
        return ResponseEntity.ok().build()
    }
}
