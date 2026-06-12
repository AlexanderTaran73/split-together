package com.splittogether.backend.storage.service

import org.springframework.stereotype.Component

@Component
class AvatarUrlResolver(private val storageService: StorageService) {
    fun resolve(objectKey: String?): String? = objectKey?.let(storageService::presignedGetUrl)
}
