package com.splittogether.backend.group.repository

import com.splittogether.backend.group.entity.InvitationType
import org.springframework.data.jpa.repository.JpaRepository

interface InvitationTypeRepository : JpaRepository<InvitationType, Int> {
    fun findByCode(code: String): InvitationType?
}
