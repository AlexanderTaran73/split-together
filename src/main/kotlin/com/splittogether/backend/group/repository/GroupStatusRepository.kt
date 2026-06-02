package com.splittogether.backend.group.repository

import com.splittogether.backend.group.entity.GroupStatus
import org.springframework.data.jpa.repository.JpaRepository

interface GroupStatusRepository : JpaRepository<GroupStatus, Int> {
    fun findByCode(code: String): GroupStatus?
}
