package com.splittogether.backend.file.controller

import com.splittogether.backend.auth.security.AppUserDetails
import com.splittogether.backend.file.dto.FileResponse
import com.splittogether.backend.file.service.GroupFileService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/groups/{groupId}/files")
class GroupFileController(private val groupFileService: GroupFileService) {

    @PostMapping(consumes = ["multipart/form-data"])
    fun upload(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false) description: String?
    ): ResponseEntity<FileResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(groupFileService.upload(user.userId, groupId, file, description))

    @GetMapping
    fun list(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<List<FileResponse>> =
        ResponseEntity.ok(groupFileService.list(user.userId, groupId))

    @GetMapping("/{fileId}")
    fun get(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @PathVariable fileId: Long
    ): ResponseEntity<FileResponse> =
        ResponseEntity.ok(groupFileService.get(user.userId, groupId, fileId))

    @DeleteMapping("/{fileId}")
    fun delete(
        @AuthenticationPrincipal user: AppUserDetails,
        @PathVariable groupId: Long,
        @PathVariable fileId: Long
    ): ResponseEntity<Void> {
        groupFileService.delete(user.userId, groupId, fileId)
        return ResponseEntity.noContent().build()
    }
}
