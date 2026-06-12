package com.splittogether.backend.storage.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.NoSuchBucketException

@Component
class StorageInitializer(
    private val s3: S3Client,
    private val props: StorageProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun ensureBucket() {
        // MinIO может стартовать чуть позже приложения — несколько попыток с паузой
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                createBucketIfAbsent()
                return
            } catch (e: Exception) {
                if (attempt == MAX_ATTEMPTS - 1) throw e
                log.warn("MinIO not ready (attempt ${attempt + 1}/$MAX_ATTEMPTS): ${e.message}")
                Thread.sleep(RETRY_DELAY_MS)
            }
        }
    }

    private fun createBucketIfAbsent() {
        val exists = try {
            s3.headBucket { it.bucket(props.bucket) }
            true
        } catch (e: NoSuchBucketException) {
            false
        }
        if (!exists) {
            s3.createBucket { it.bucket(props.bucket) }
            log.info("Created MinIO bucket '${props.bucket}'")
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 10
        const val RETRY_DELAY_MS = 2000L
    }
}
