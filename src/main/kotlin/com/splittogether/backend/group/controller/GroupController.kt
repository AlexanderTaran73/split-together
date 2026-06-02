package com.splittogether.backend.group.controller

import com.splittogether.backend.auth.security.AppUserDetails
import com.splittogether.backend.group.dto.*
import com.splittogether.backend.group.service.GroupService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/groups")
class GroupController(private val groupService: GroupService) {

    @PostMapping
    fun createGroup(
        @AuthenticationPrincipal user: AppUserDetails,
        @Valid @RequestBody request: CreateGroupRequest
    ): ResponseEntity<GroupResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(user.userId, request))

    @GetMapping
    fun getMyGroups(@AuthenticationPrincipal user: AppUserDetails): ResponseEntity<List<GroupResponse>> =
        ResponseEntity.ok(groupService.getMyGroups(user.userId))

    @GetMapping("/{groupId}")
    fun getGroup(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<GroupResponse> =
        ResponseEntity.ok(groupService.getGroup(user.userId, groupId))

    @PutMapping("/{groupId}")
    fun updateGroup(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: UpdateGroupRequest
    ): ResponseEntity<GroupResponse> =
        ResponseEntity.ok(groupService.updateGroup(user.userId, groupId, request))

    @DeleteMapping("/{groupId}")
    fun archiveGroup(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<Void> {
        groupService.archiveGroup(user.userId, groupId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{groupId}/members")
    fun getMembers(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<List<GroupMemberResponse>> =
        ResponseEntity.ok(groupService.getMembers(user.userId, groupId))

    @DeleteMapping("/{groupId}/members/{userId}")
    fun removeMember(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @PathVariable userId: Long
    ): ResponseEntity<Void> {
        groupService.removeMember(user.userId, groupId, userId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{groupId}/members/{userId}/role")
    fun updateMemberRole(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @PathVariable userId: Long,
        @Valid @RequestBody request: UpdateMemberRoleRequest
    ): ResponseEntity<GroupMemberResponse> =
        ResponseEntity.ok(groupService.updateMemberRole(user.userId, groupId, userId, request))

    @PutMapping("/{groupId}/owner")
    fun transferOwnership(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @RequestParam newOwnerId: Long
    ): ResponseEntity<GroupMemberResponse> =
        ResponseEntity.ok(groupService.transferOwnership(user.userId, groupId, newOwnerId))

    @PostMapping("/{groupId}/invitations")
    fun createInvitation(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: CreateInvitationRequest
    ): ResponseEntity<GroupInvitationResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(groupService.createInvitation(user.userId, groupId, request))

    @GetMapping("/{groupId}/invitations")
    fun getInvitations(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<List<GroupInvitationResponse>> =
        ResponseEntity.ok(groupService.getInvitations(user.userId, groupId))

    @DeleteMapping("/{groupId}/invitations/{invitationId}")
    fun revokeInvitation(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @PathVariable invitationId: Long
    ): ResponseEntity<Void> {
        groupService.revokeInvitation(user.userId, groupId, invitationId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/join")
    fun joinGroup(
        @AuthenticationPrincipal user: AppUserDetails,
        @Valid @RequestBody request: JoinGroupRequest
    ): ResponseEntity<GroupResponse> =
        ResponseEntity.ok(groupService.joinGroup(user.userId, request))
}
