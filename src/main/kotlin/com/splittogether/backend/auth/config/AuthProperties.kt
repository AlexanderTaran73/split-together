package com.splittogether.backend.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "auth")
data class AuthProperties(
    val jwt: Jwt = Jwt(),
    val verification: Verification = Verification()
) {
    data class Jwt(
        val secret: String = "",
        val accessTokenExpiry: Long = 900000,
        val refreshTokenExpiryDays: Long = 30
    )

    data class Verification(
        val fixedCode: String = ""
    )
}
