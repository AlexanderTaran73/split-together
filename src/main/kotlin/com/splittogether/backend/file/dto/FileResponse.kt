package com.splittogether.backend.file.dto

import java.time.Instant

data class FileResponse(
    val id: Long,
    val originalName: String,
    val contentType: String?,
    val sizeBytes: Long,
    val description: String?,
    val url: String,
    val uploadedByUserId: Long,
    val createdAt: Instant
)
