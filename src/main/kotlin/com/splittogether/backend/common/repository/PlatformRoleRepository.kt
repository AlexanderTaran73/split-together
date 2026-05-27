package com.splittogether.backend.common.repository

import com.splittogether.backend.common.entity.PlatformRole
import org.springframework.data.jpa.repository.JpaRepository

interface PlatformRoleRepository : JpaRepository<PlatformRole, Int> {
    fun findByCode(code: String): PlatformRole?
}
