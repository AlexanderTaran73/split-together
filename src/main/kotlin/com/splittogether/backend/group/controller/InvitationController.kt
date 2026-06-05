package com.splittogether.backend.group.controller

import com.splittogether.backend.auth.security.AppUserDetails
import com.splittogether.backend.group.dto.GroupResponse
import com.splittogether.backend.group.service.GroupService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/invitations")
class InvitationController(private val groupService: GroupService) {

    @PostMapping("/{id}/accept")
    fun acceptInvitation(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable id: Long
    ): ResponseEntity<GroupResponse> =
        ResponseEntity.ok(groupService.acceptInvitation(user.userId, id))

    @PostMapping("/{id}/reject")
    fun rejectInvitation(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        groupService.rejectInvitation(user.userId, id)
        return ResponseEntity.noContent().build()
    }
}
