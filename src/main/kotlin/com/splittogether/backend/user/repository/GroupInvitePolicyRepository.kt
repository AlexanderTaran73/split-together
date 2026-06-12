package com.splittogether.backend.user.repository

import com.splittogether.backend.user.entity.GroupInvitePolicy
import org.springframework.data.jpa.repository.JpaRepository

interface GroupInvitePolicyRepository : JpaRepository<GroupInvitePolicy, Int> {
    fun findByCode(code: String): GroupInvitePolicy?
}
