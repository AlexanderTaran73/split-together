package com.splittogether.backend

import com.splittogether.backend.email.CapturingMailSender
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.testcontainers.containers.PostgreSQLContainer

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {

    @Bean
    @ServiceConnection
    fun postgres(): PostgreSQLContainer<*> =
        PostgreSQLContainer<Nothing>("postgres:16-alpine")

    @Bean
    @Primary
    fun capturingMailSender(): CapturingMailSender = CapturingMailSender()
}
