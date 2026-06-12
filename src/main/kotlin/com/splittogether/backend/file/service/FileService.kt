package com.splittogether.backend.file.service

import com.splittogether.backend.common.exception.InsufficientPermissionsException
import com.splittogether.backend.common.exception.StoredFileNotFoundException
import com.splittogether.backend.file.dto.FileResponse
import com.splittogether.backend.file.entity.StoredFile
import com.splittogether.backend.file.repository.FileOwnerTypeRepository
import com.splittogether.backend.file.repository.StoredFileRepository
import com.splittogether.backend.storage.FileConstraints
import com.splittogether.backend.storage.service.FileValidator
import com.splittogether.backend.storage.service.StorageService
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.UUID

@Service
class FileService(
    private val storedFileRepository: StoredFileRepository,
    private val fileOwnerTypeRepository: FileOwnerTypeRepository,
    private val storageService: StorageService,
    private val fileValidator: FileValidator,
    private val userRepository: UserRepository
) {

    @Transactional
    fun upload(
        ownerTypeCode: String,
        ownerId: Long,
        uploaderId: Long,
        file: MultipartFile,
        description: String?,
        keyPrefix: String
    ): FileResponse {
        fileValidator.validate(file, FileConstraints.FILE_MAX_BYTES)

        val ownerType = fileOwnerTypeRepository.findByCode(ownerTypeCode)
            ?: error("Missing reference data: file_owner_type=$ownerTypeCode")
        val uploader = userRepository.getReferenceById(uploaderId)
        val key = "$keyPrefix/${UUID.randomUUID()}"
        storageService.upload(key, file.bytes, file.contentType)

        val saved = storedFileRepository.save(
            StoredFile(
                ownerType = ownerType,
                ownerId = ownerId,
                uploadedBy = uploader,
                objectKey = key,
                originalName = file.originalFilename ?: key.substringAfterLast('/'),
                contentType = file.contentType,
                sizeBytes = file.size,
                description = description
            )
        )
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun list(ownerTypeCode: String, ownerId: Long): List<FileResponse> =
        storedFileRepository.findActiveByOwner(ownerTypeCode, ownerId).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun get(ownerTypeCode: String, ownerId: Long, fileId: Long): FileResponse =
        requireOwned(fileId, ownerTypeCode, ownerId).toResponse()

    @Transactional
    fun softDelete(ownerTypeCode: String, ownerId: Long, fileId: Long, requesterId: Long, canManage: Boolean) {
        val file = requireOwned(fileId, ownerTypeCode, ownerId)
        if (file.uploadedBy.id != requesterId && !canManage)
            throw InsufficientPermissionsException("You can delete only files you uploaded")
        file.deletedAt = Instant.now()
        file.deletedBy = userRepository.getReferenceById(requesterId)
        storedFileRepository.save(file)
    }

    private fun requireOwned(fileId: Long, ownerTypeCode: String, ownerId: Long): StoredFile {
        val file = storedFileRepository.findActiveById(fileId)
            ?: throw StoredFileNotFoundException("File not found")
        if (file.ownerType.code != ownerTypeCode || file.ownerId != ownerId)
            throw StoredFileNotFoundException("File not found")
        return file
    }

    private fun StoredFile.toResponse() = FileResponse(
        id = id,
        originalName = originalName,
        contentType = contentType,
        sizeBytes = sizeBytes,
        description = description,
        url = storageService.presignedGetUrl(objectKey),
        uploadedByUserId = uploadedBy.id,
        createdAt = createdAt
    )
}
