package com.splittogether.backend.group.repository

import com.splittogether.backend.group.entity.GroupInvitation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GroupInvitationRepository : JpaRepository<GroupInvitation, Long> {

    fun findByInviteCode(inviteCode: String): GroupInvitation?

    @Query("SELECT i FROM GroupInvitation i WHERE i.group.id = :groupId AND i.status.code = 'PENDING'")
    fun findPendingByGroupId(groupId: Long): List<GroupInvitation>

    @Query("""
        SELECT i FROM GroupInvitation i
        WHERE i.invitedUser.id = :userId
        AND i.status.code = 'PENDING'
        AND i.type.code = 'DIRECT'
        AND (i.expiresAt IS NULL OR i.expiresAt > :now)
    """)
    fun findPendingDirectByInvitedUserId(userId: Long, now: java.time.Instant): List<GroupInvitation>
}
