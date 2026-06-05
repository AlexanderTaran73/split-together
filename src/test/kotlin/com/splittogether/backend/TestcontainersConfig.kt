package com.splittogether.backend

import com.splittogether.backend.email.CapturingMailSender
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.task.SyncTaskExecutor
import org.testcontainers.containers.PostgreSQLContainer
import java.util.concurrent.Executor

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {

    @Bean
    @ServiceConnection
    fun postgres(): PostgreSQLContainer<*> =
        PostgreSQLContainer<Nothing>("postgres:16-alpine")

    @Bean
    @Primary
    fun capturingMailSender(): CapturingMailSender = CapturingMailSender()

    // run email tasks synchronously so tests can assert on sent emails immediately
    @Bean("emailExecutor")
    @Primary
    fun emailExecutor(): Executor = SyncTaskExecutor()
}
