package com.splittogether.backend.group.repository

import com.splittogether.backend.group.entity.InvitationUse
import org.springframework.data.jpa.repository.JpaRepository

interface InvitationUseRepository : JpaRepository<InvitationUse, Long> {
    fun countByInvitationId(invitationId: Long): Long
    fun deleteByInvitationId(invitationId: Long)
}
