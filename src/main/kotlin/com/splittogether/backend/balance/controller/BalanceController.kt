package com.splittogether.backend.balance.controller

import com.splittogether.backend.auth.security.AppUserDetails
import com.splittogether.backend.balance.dto.BalanceEntryResponse
import com.splittogether.backend.balance.dto.SimplifiedDebtResponse
import com.splittogether.backend.balance.service.BalanceService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/groups/{groupId}/balances")
class BalanceController(private val balanceService: BalanceService) {

    @GetMapping
    fun getBalances(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @RequestParam(required = false) currency: String?
    ): ResponseEntity<List<BalanceEntryResponse>> =
        ResponseEntity.ok(balanceService.getBalances(user.userId, groupId, currency))

    @GetMapping("/simplified")
    fun getSimplifiedDebts(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @RequestParam(required = false) currency: String?
    ): ResponseEntity<List<SimplifiedDebtResponse>> =
        ResponseEntity.ok(balanceService.getSimplifiedDebts(user.userId, groupId, currency))

    @PostMapping("/simplify")
    fun simplifyBalances(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @RequestParam(required = false) currency: String?
    ): ResponseEntity<List<BalanceEntryResponse>> =
        ResponseEntity.ok(balanceService.simplifyBalances(user.userId, groupId, currency))
}
