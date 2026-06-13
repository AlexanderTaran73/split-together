package com.splittogether.backend.notification.device.dto

import jakarta.validation.constraints.NotBlank

data class RegisterDeviceRequest(
    @field:NotBlank(message = "token is required")
    val token: String,

    @field:NotBlank(message = "platform is required")
    val platform: String
)
