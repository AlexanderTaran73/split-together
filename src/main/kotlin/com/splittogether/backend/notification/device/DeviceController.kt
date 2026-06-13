package com.splittogether.backend.notification.device

import com.splittogether.backend.auth.security.AppUserDetails
import com.splittogether.backend.notification.device.dto.RegisterDeviceRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users/me/devices")
class DeviceController(private val deviceTokenService: DeviceTokenService) {

    @PostMapping
    fun register(
        @AuthenticationPrincipal user: AppUserDetails,
        @Valid @RequestBody request: RegisterDeviceRequest
    ): ResponseEntity<Void> {
        deviceTokenService.register(user.userId, request)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping
    fun revoke(
        @AuthenticationPrincipal user: AppUserDetails,
        @RequestParam token: String
    ): ResponseEntity<Void> {
        deviceTokenService.revoke(user.userId, token)
        return ResponseEntity.noContent().build()
    }
}
