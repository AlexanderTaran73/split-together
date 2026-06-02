package com.splittogether.backend.group.repository

import com.splittogether.backend.group.entity.GroupRole
import org.springframework.data.jpa.repository.JpaRepository

interface GroupRoleRepository : JpaRepository<GroupRole, Int> {
    fun findByCode(code: String): GroupRole?
}
