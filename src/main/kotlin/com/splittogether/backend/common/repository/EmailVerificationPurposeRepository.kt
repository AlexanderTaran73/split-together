package com.splittogether.backend.common.repository

import com.splittogether.backend.common.entity.EmailVerificationPurpose
import org.springframework.data.jpa.repository.JpaRepository

interface EmailVerificationPurposeRepository : JpaRepository<EmailVerificationPurpose, Int> {
    fun findByCode(code: String): EmailVerificationPurpose?
}
