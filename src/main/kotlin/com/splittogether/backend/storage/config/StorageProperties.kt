package com.splittogether.backend.storage.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "storage")
data class StorageProperties(
    val internalEndpoint: String = "http://localhost:9000",
    val publicEndpoint: String = "http://localhost:9000",
    val accessKey: String = "minioadmin",
    val secretKey: String = "minioadmin",
    val region: String = "us-east-1",
    val bucket: String = "splittogether",
    val presignTtlSeconds: Long = 900
)
