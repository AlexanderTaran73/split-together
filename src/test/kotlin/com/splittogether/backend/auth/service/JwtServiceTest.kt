package com.splittogether.backend.auth.service

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.user.entity.User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JwtServiceTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var jwtService: JwtService

    @Value("\${app.jwt.secret}")
    private lateinit var secret: String

    private val testUser = User(id = 42L, email = "jwt@test.com", passwordHash = "hash", displayName = "JWT Test")

    @Test
    fun `generateAccessToken contains correct email as subject`() {
        val token = jwtService.generateAccessToken(testUser)
        assertEquals("jwt@test.com", jwtService.extractEmail(token))
    }

    @Test
    fun `generateAccessToken contains userId claim`() {
        val token = jwtService.generateAccessToken(testUser)
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        assertEquals(42L, (claims["userId"] as Number).toLong())
    }

    @Test
    fun `extractEmail returns null for malformed token`() {
        assertNull(jwtService.extractEmail("not.a.valid.token"))
    }

    @Test
    fun `isValid returns true for freshly generated token`() {
        val token = jwtService.generateAccessToken(testUser)
        assertTrue(jwtService.isValid(token))
    }

    @Test
    fun `isValid returns false for expired token`() {
        assertFalse(jwtService.isValid(expiredToken()))
    }

    @Test
    fun `isValid returns false for tampered signature`() {
        val token = jwtService.generateAccessToken(testUser)
        val tampered = token.dropLast(6) + "XXXXXX"
        assertFalse(jwtService.isValid(tampered))
    }

    private fun expiredToken(): String {
        val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
        return Jwts.builder()
            .subject(testUser.email)
            .claim("userId", testUser.id)
            .issuedAt(Date(System.currentTimeMillis() - 60_000))
            .expiration(Date(System.currentTimeMillis() - 1_000))
            .signWith(key)
            .compact()
    }
}
