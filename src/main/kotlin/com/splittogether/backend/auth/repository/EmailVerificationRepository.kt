package com.splittogether.backend.auth.repository

import com.splittogether.backend.auth.entity.EmailVerification
import com.splittogether.backend.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EmailVerificationRepository : JpaRepository<EmailVerification, Long> {

    @Query("""
        SELECT ev FROM EmailVerification ev
        WHERE ev.user = :user AND ev.purpose.code = :purposeCode AND ev.usedAt IS NULL
        ORDER BY ev.createdAt DESC
        LIMIT 1
    """)
    fun findLatestUnused(
        @Param("user") user: User,
        @Param("purposeCode") purposeCode: String
    ): EmailVerification?
}
