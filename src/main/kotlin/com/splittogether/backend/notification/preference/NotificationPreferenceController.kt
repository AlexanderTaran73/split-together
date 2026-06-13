package com.splittogether.backend.notification.preference

import com.splittogether.backend.auth.security.AppUserDetails
import com.splittogether.backend.notification.preference.dto.NotificationPreferenceResponse
import com.splittogether.backend.notification.preference.dto.UpdateNotificationPreferencesRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users/me/notification-preferences")
class NotificationPreferenceController(
    private val notificationPreferenceService: NotificationPreferenceService
) {

    @GetMapping
    fun getPreferences(
        @AuthenticationPrincipal user: AppUserDetails
    ): ResponseEntity<List<NotificationPreferenceResponse>> =
        ResponseEntity.ok(notificationPreferenceService.getPreferences(user.userId))

    @PutMapping
    fun updatePreferences(
        @AuthenticationPrincipal user: AppUserDetails,
        @Valid @RequestBody request: UpdateNotificationPreferencesRequest
    ): ResponseEntity<List<NotificationPreferenceResponse>> =
        ResponseEntity.ok(notificationPreferenceService.updatePreferences(user.userId, request))
}
