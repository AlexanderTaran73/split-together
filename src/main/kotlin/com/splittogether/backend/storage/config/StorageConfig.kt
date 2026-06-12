package com.splittogether.backend.storage.config

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
class StorageConfig(private val props: StorageProperties) {

    private fun credentials() =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(props.accessKey, props.secretKey))

    @Bean
    fun s3Client(): S3Client =
        S3Client.builder()
            .endpointOverride(URI.create(props.internalEndpoint))
            .region(Region.of(props.region))
            .credentialsProvider(credentials())
            .forcePathStyle(true)
            .build()

    @Bean
    fun s3Presigner(): S3Presigner =
        S3Presigner.builder()
            .endpointOverride(URI.create(props.publicEndpoint))
            .region(Region.of(props.region))
            .credentialsProvider(credentials())
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build()
}
