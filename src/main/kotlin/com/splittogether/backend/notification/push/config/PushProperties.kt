package com.splittogether.backend.notification.push.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "push")
data class PushProperties(
    val enabled: Boolean = false,
    val credentialsPath: String = ""
)
