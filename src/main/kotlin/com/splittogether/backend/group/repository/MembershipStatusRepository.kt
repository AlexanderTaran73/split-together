package com.splittogether.backend.group.repository

import com.splittogether.backend.group.entity.MembershipStatus
import org.springframework.data.jpa.repository.JpaRepository

interface MembershipStatusRepository : JpaRepository<MembershipStatus, Int> {
    fun findByCode(code: String): MembershipStatus?
}
