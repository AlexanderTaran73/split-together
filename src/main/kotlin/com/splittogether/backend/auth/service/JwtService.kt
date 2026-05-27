package com.splittogether.backend.auth.service

import com.splittogether.backend.user.entity.User
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date

@Service
class JwtService {

    @Value("\${app.jwt.secret}")
    private lateinit var secret: String

    @Value("\${app.jwt.access-token-expiry}")
    private var accessTokenExpiry: Long = 0

    fun generateAccessToken(user: User): String {
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
        return Jwts.builder()
            .subject(user.email)
            .claim("userId", user.id)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + accessTokenExpiry))
            .signWith(key)
            .compact()
    }

    fun extractEmail(token: String): String? = try {
        parseClaims(token).subject
    } catch (e: JwtException) {
        null
    }

    fun isValid(token: String): Boolean = try {
        parseClaims(token)
        true
    } catch (e: JwtException) {
        false
    }

    private fun parseClaims(token: String) =
        Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
            .build()
            .parseSignedClaims(token)
            .payload
}
