package com.splittogether.backend.user.controller

import com.splittogether.backend.auth.security.AppUserDetails
import com.splittogether.backend.group.dto.GroupResponse
import com.splittogether.backend.group.dto.IncomingInvitationResponse
import com.splittogether.backend.user.dto.UpdatePrivacyRequest
import com.splittogether.backend.user.dto.UpdateProfileRequest
import com.splittogether.backend.user.dto.UserBalanceResponse
import com.splittogether.backend.user.dto.UserPrivacyResponse
import com.splittogether.backend.user.dto.UserResponse
import com.splittogether.backend.user.service.UserService
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/users")
@Validated
class UserController(private val userService: UserService) {

    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal user: AppUserDetails): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.getMe(user.userId))

    @GetMapping
    fun search(
        @AuthenticationPrincipal user: AppUserDetails,
        @RequestParam @Size(min = 2, message = "Query must be at least 2 characters") query: String
    ): ResponseEntity<List<UserResponse>> =
        ResponseEntity.ok(userService.search(user.userId, query))

    @GetMapping("/me/groups")
    fun getMyGroups(@AuthenticationPrincipal user: AppUserDetails): ResponseEntity<List<GroupResponse>> =
        ResponseEntity.ok(userService.getMyGroups(user.userId))

    @GetMapping("/me/balance")
    fun getMyBalance(
        @AuthenticationPrincipal user: AppUserDetails,
        @RequestParam(required = false) currency: String?
    ): ResponseEntity<UserBalanceResponse> =
        ResponseEntity.ok(userService.getMyBalance(user.userId, currency))

    @GetMapping("/me/invitations")
    fun getMyInvitations(@AuthenticationPrincipal user: AppUserDetails): ResponseEntity<List<IncomingInvitationResponse>> =
        ResponseEntity.ok(userService.getMyInvitations(user.userId))

    @PutMapping("/me")
    fun updateMe(
        @AuthenticationPrincipal user: AppUserDetails,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.updateMe(user.userId, request))

    @PutMapping("/me/avatar", consumes = ["multipart/form-data"])
    fun updateAvatar(
        @AuthenticationPrincipal user: AppUserDetails,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.updateAvatar(user.userId, file))

    @DeleteMapping("/me/avatar")
    fun deleteAvatar(@AuthenticationPrincipal user: AppUserDetails): ResponseEntity<Void> {
        userService.deleteAvatar(user.userId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me/privacy")
    fun getPrivacy(@AuthenticationPrincipal user: AppUserDetails): ResponseEntity<UserPrivacyResponse> =
        ResponseEntity.ok(userService.getPrivacy(user.userId))

    @PutMapping("/me/privacy")
    fun updatePrivacy(
        @AuthenticationPrincipal user: AppUserDetails,
        @Valid @RequestBody request: UpdatePrivacyRequest
    ): ResponseEntity<UserPrivacyResponse> =
        ResponseEntity.ok(userService.updatePrivacy(user.userId, request))
}
