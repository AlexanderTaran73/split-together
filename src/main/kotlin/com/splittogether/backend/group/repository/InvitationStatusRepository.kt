package com.splittogether.backend.group.repository

import com.splittogether.backend.group.entity.InvitationStatus
import org.springframework.data.jpa.repository.JpaRepository

interface InvitationStatusRepository : JpaRepository<InvitationStatus, Int> {
    fun findByCode(code: String): InvitationStatus?
}
