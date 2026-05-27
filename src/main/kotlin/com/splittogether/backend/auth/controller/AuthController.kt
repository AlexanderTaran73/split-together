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
}
