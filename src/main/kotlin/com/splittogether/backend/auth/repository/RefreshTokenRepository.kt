package com.splittogether.backend.auth.repository

import com.splittogether.backend.auth.entity.RefreshToken
import com.splittogether.backend.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenHashAndRevokedAtIsNull(tokenHash: String): RefreshToken?

    @Modifying
    @Query("""
        UPDATE RefreshToken t 
        SET t.revokedAt = :now 
        WHERE t.user = :user AND t.revokedAt IS NULL
    """)
    fun revokeAllActiveByUser(@Param("user") user: User, @Param("now") now: Instant)
}
