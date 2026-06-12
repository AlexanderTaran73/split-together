package com.splittogether.backend.group.service

import com.splittogether.backend.common.exception.InsufficientPermissionsException
import com.splittogether.backend.common.exception.NotGroupMemberException
import com.splittogether.backend.group.entity.GroupMember
import com.splittogether.backend.group.entity.GroupRole
import com.splittogether.backend.group.entity.MembershipStatus
import com.splittogether.backend.group.repository.GroupMemberRepository
import org.springframework.stereotype.Component

@Component
class MembershipGuard(private val groupMemberRepository: GroupMemberRepository) {

    fun requireActiveMember(groupId: Long, userId: Long): GroupMember =
        groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            ?.takeIf { it.status.code == MembershipStatus.ACTIVE }
            ?: throw NotGroupMemberException("You are not a member of this group")

    fun requireAdminOrOwner(member: GroupMember) {
        if (member.role.code == GroupRole.MEMBER)
            throw InsufficientPermissionsException("Admin or Owner role required")
    }
}
