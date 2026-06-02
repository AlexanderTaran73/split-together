package com.splittogether.backend.group.repository

import com.splittogether.backend.group.entity.GroupMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GroupMemberRepository : JpaRepository<GroupMember, Long> {

    fun findByGroupIdAndUserId(groupId: Long, userId: Long): GroupMember?

    @Query("SELECT m FROM GroupMember m WHERE m.group.id = :groupId AND m.status.code = 'ACTIVE'")
    fun findActiveMembersByGroupId(groupId: Long): List<GroupMember>

    @Query("SELECT m FROM GroupMember m WHERE m.user.id = :userId AND m.status.code = 'ACTIVE'")
    fun findActiveByUserId(userId: Long): List<GroupMember>

    @Query("SELECT COUNT(m) FROM GroupMember m WHERE m.group.id = :groupId AND m.status.code = 'ACTIVE'")
    fun countActiveMembersByGroupId(groupId: Long): Long
}
