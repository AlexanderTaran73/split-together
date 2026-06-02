package com.splittogether.backend.group.repository

import com.splittogether.backend.group.entity.GroupInvitation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GroupInvitationRepository : JpaRepository<GroupInvitation, Long> {

    fun findByInviteCode(inviteCode: String): GroupInvitation?

    @Query("SELECT i FROM GroupInvitation i WHERE i.group.id = :groupId AND i.status.code = 'PENDING'")
    fun findPendingByGroupId(groupId: Long): List<GroupInvitation>
}
