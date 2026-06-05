package com.splittogether.backend.settlement.controller

import com.splittogether.backend.auth.security.AppUserDetails
import com.splittogether.backend.settlement.dto.CreateSettlementRequest
import com.splittogether.backend.settlement.dto.RejectSettlementRequest
import com.splittogether.backend.settlement.dto.SettlementResponse
import com.splittogether.backend.settlement.service.SettlementService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/groups/{groupId}/settlements")
class SettlementController(private val settlementService: SettlementService) {

    @PostMapping
    fun createSettlement(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: CreateSettlementRequest
    ): ResponseEntity<SettlementResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(settlementService.createSettlement(user.userId, groupId, request))

    @GetMapping
    fun getSettlements(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<List<SettlementResponse>> =
        ResponseEntity.ok(settlementService.getSettlements(user.userId, groupId))

    @PostMapping("/{settlementId}/confirm")
    fun confirmSettlement(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @PathVariable settlementId: Long
    ): ResponseEntity<SettlementResponse> =
        ResponseEntity.ok(settlementService.confirmSettlement(user.userId, groupId, settlementId))

    @PostMapping("/{settlementId}/reject")
    fun rejectSettlement(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @PathVariable settlementId: Long,
        @RequestBody(required = false) request: RejectSettlementRequest?
    ): ResponseEntity<SettlementResponse> =
        ResponseEntity.ok(settlementService.rejectSettlement(user.userId, groupId, settlementId, request?.rejectionReason))
}
