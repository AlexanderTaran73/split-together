package com.splittogether.backend.storage.service

import com.splittogether.backend.storage.config.StorageProperties
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration

@Service
class StorageService(
    private val s3: S3Client,
    private val presigner: S3Presigner,
    private val props: StorageProperties
) {

    fun upload(key: String, bytes: ByteArray, contentType: String?) {
        s3.putObject(
            PutObjectRequest.builder()
                .bucket(props.bucket)
                .key(key)
                .contentType(contentType ?: "application/octet-stream")
                .build(),
            RequestBody.fromBytes(bytes)
        )
    }

    fun presignedGetUrl(key: String): String =
        presigner.presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(props.presignTtlSeconds))
                .getObjectRequest(GetObjectRequest.builder().bucket(props.bucket).key(key).build())
                .build()
        ).url().toString()

    fun delete(key: String) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(props.bucket).key(key).build())
    }
}
