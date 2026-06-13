package com.splittogether.backend.group.repository

import com.splittogether.backend.group.entity.GroupMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GroupMemberRepository : JpaRepository<GroupMember, Long> {

    interface MemberCountProjection {
        val groupId: Long
        val count: Long
    }

    fun findByGroupIdAndUserId(groupId: Long, userId: Long): GroupMember?

    @Query("SELECT m FROM GroupMember m JOIN FETCH m.user WHERE m.group.id = :groupId AND m.status.code = 'ACTIVE'")
    fun findActiveMembersByGroupId(groupId: Long): List<GroupMember>

    @Query("SELECT m FROM GroupMember m JOIN FETCH m.group g JOIN FETCH g.owner JOIN FETCH g.baseCurrency WHERE m.user.id = :userId AND m.status.code = 'ACTIVE'")
    fun findActiveByUserId(userId: Long): List<GroupMember>

    @Query("SELECT COUNT(m) FROM GroupMember m WHERE m.group.id = :groupId AND m.status.code = 'ACTIVE'")
    fun countActiveMembersByGroupId(groupId: Long): Long

    @Query("SELECT m.group.id as groupId, COUNT(m) as count FROM GroupMember m WHERE m.group.id IN :groupIds AND m.status.code = 'ACTIVE' GROUP BY m.group.id")
    fun countActiveMembersByGroupIds(groupIds: List<Long>): List<MemberCountProjection>
}
