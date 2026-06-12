package com.splittogether.backend.file

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.storage.service.StorageService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.net.URI
import java.util.UUID
import kotlin.test.assertContentEquals

class StorageServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var storageService: StorageService

    @Test
    fun `upload then presigned GET returns the same bytes`() {
        val key = "test/${UUID.randomUUID()}"
        val content = "hello storage".toByteArray()

        storageService.upload(key, content, "text/plain")
        val url = storageService.presignedGetUrl(key)
        val fetched = URI(url).toURL().openStream().use { it.readBytes() }

        assertContentEquals(content, fetched)
    }
}
