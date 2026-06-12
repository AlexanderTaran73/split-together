package com.splittogether.backend.friendship.controller

import com.splittogether.backend.auth.security.AppUserDetails
import com.splittogether.backend.friendship.dto.FriendRequestResponse
import com.splittogether.backend.friendship.dto.FriendResponse
import com.splittogether.backend.friendship.dto.SendFriendRequest
import com.splittogether.backend.friendship.service.FriendshipService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/friends")
class FriendshipController(private val friendshipService: FriendshipService) {

    @GetMapping
    fun getFriends(@AuthenticationPrincipal user: AppUserDetails): ResponseEntity<List<FriendResponse>> =
        ResponseEntity.ok(friendshipService.getFriends(user.userId))

    @GetMapping("/requests/incoming")
    fun getIncoming(@AuthenticationPrincipal user: AppUserDetails): ResponseEntity<List<FriendRequestResponse>> =
        ResponseEntity.ok(friendshipService.getIncomingRequests(user.userId))

    @GetMapping("/requests/outgoing")
    fun getOutgoing(@AuthenticationPrincipal user: AppUserDetails): ResponseEntity<List<FriendRequestResponse>> =
        ResponseEntity.ok(friendshipService.getOutgoingRequests(user.userId))

    @PostMapping("/requests")
    fun sendRequest(
        @AuthenticationPrincipal user: AppUserDetails,
        @Valid @RequestBody request: SendFriendRequest
    ): ResponseEntity<FriendRequestResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(friendshipService.sendRequest(user.userId, request.userId))

    @PostMapping("/requests/{id}/accept")
    fun acceptRequest(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable id: Long
    ): ResponseEntity<FriendResponse> =
        ResponseEntity.ok(friendshipService.acceptRequest(user.userId, id))

    @PostMapping("/requests/{id}/decline")
    fun declineRequest(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        friendshipService.declineRequest(user.userId, id)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{userId}")
    fun removeFriend(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable userId: Long
    ): ResponseEntity<Void> {
        friendshipService.removeFriend(user.userId, userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{userId}/block")
    fun block(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable userId: Long
    ): ResponseEntity<Void> {
        friendshipService.block(user.userId, userId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{userId}/block")
    fun unblock(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable userId: Long
    ): ResponseEntity<Void> {
        friendshipService.unblock(user.userId, userId)
        return ResponseEntity.noContent().build()
    }
}
