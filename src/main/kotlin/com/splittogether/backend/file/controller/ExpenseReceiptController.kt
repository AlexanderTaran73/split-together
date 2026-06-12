package com.splittogether.backend.file.controller

import com.splittogether.backend.auth.security.AppUserDetails
import com.splittogether.backend.file.dto.FileResponse
import com.splittogether.backend.file.service.ExpenseReceiptService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/groups/{groupId}/expenses/{expenseId}/receipts")
class ExpenseReceiptController(private val expenseReceiptService: ExpenseReceiptService) {

    @PostMapping(consumes = ["multipart/form-data"])
    fun upload(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @PathVariable expenseId: Long,
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false) description: String?
    ): ResponseEntity<FileResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(expenseReceiptService.upload(user.userId, groupId, expenseId, file, description))

    @GetMapping
    fun list(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @PathVariable expenseId: Long
    ): ResponseEntity<List<FileResponse>> =
        ResponseEntity.ok(expenseReceiptService.list(user.userId, groupId, expenseId))

    @GetMapping("/{fileId}")
    fun get(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @PathVariable expenseId: Long,
        @PathVariable fileId: Long
    ): ResponseEntity<FileResponse> =
        ResponseEntity.ok(expenseReceiptService.get(user.userId, groupId, expenseId, fileId))

    @DeleteMapping("/{fileId}")
    fun delete(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @PathVariable expenseId: Long,
        @PathVariable fileId: Long
    ): ResponseEntity<Void> {
        expenseReceiptService.delete(user.userId, groupId, expenseId, fileId)
        return ResponseEntity.noContent().build()
    }
}
