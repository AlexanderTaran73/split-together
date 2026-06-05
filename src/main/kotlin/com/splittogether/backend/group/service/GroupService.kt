package com.splittogether.backend.group.service

import com.splittogether.backend.balance.service.BalanceService
import com.splittogether.backend.common.exception.*
import com.splittogether.backend.common.repository.CurrencyRepository
import com.splittogether.backend.group.dto.*
import com.splittogether.backend.group.entity.*
import com.splittogether.backend.group.repository.*
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupInvitationRepository: GroupInvitationRepository,
    private val invitationUseRepository: InvitationUseRepository,
    private val userRepository: UserRepository,
    private val currencyRepository: CurrencyRepository,
    private val groupRoleRepository: GroupRoleRepository,
    private val groupStatusRepository: GroupStatusRepository,
    private val membershipStatusRepository: MembershipStatusRepository,
    private val invitationTypeRepository: InvitationTypeRepository,
    private val invitationStatusRepository: InvitationStatusRepository,
    private val balanceService: BalanceService
) {

    private fun groupRole(code: String): GroupRole =
        groupRoleRepository.findByCode(code) ?: error("Missing reference data: group_role=$code")

    private fun groupStatus(code: String): GroupStatus =
        groupStatusRepository.findByCode(code) ?: error("Missing reference data: group_status=$code")

    private fun memberStatus(code: String): MembershipStatus =
        membershipStatusRepository.findByCode(code) ?: error("Missing reference data: membership_status=$code")

    private fun invType(code: String): InvitationType =
        invitationTypeRepository.findByCode(code) ?: error("Missing reference data: invitation_type=$code")

    private fun invStatus(code: String): InvitationStatus =
        invitationStatusRepository.findByCode(code) ?: error("Missing reference data: invitation_status=$code")

    private fun requireActiveMember(groupId: Long, userId: Long): GroupMember =
        groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            ?.takeIf { it.status.code == MembershipStatus.ACTIVE }
            ?: throw NotGroupMemberException("You are not a member of this group")

    private fun requireAdminOrOwner(member: GroupMember) {
        if (member.role.code == GroupRole.MEMBER)
            throw InsufficientPermissionsException("Admin or Owner role required")
    }

    private fun requireOwner(member: GroupMember) {
        if (member.role.code != GroupRole.OWNER)
            throw InsufficientPermissionsException("Owner role required")
    }

    private fun requireActiveGroup(group: Group) {
        if (group.status.code != GroupStatus.ACTIVE)
            throw GroupArchivedException("Group is archived")
    }

    private fun Group.toResponse(userId: Long): GroupResponse = GroupResponse(
        id = id,
        name = name,
        description = description,
        currencyCode = currency.code,
        status = status.code,
        ownerId = owner.id,
        ownerDisplayName = owner.displayName,
        memberCount = groupMemberRepository.countActiveMembersByGroupId(id),
        currentUserRole = groupMemberRepository.findByGroupIdAndUserId(id, userId)?.role?.code ?: "",
        currentUserBalance = balanceService.getNetBalance(userId, id),
        createdAt = createdAt,
        updatedAt = updatedAt,
        archivedAt = archivedAt
    )

    private fun GroupMember.toResponse() = GroupMemberResponse(
        id = id,
        userId = user.id,
        displayName = user.displayName,
        avatarUrl = user.avatarUrl,
        role = role.code,
        joinedAt = joinedAt
    )

    private fun GroupInvitation.toResponse(usesCount: Long) = GroupInvitationResponse(
        id = id,
        type = type.code,
        status = status.code,
        inviteCode = inviteCode,
        invitedUserId = invitedUser?.id,
        maxUses = maxUses,
        usesCount = usesCount,
        expiresAt = expiresAt,
        createdAt = createdAt
    )

    @Transactional
    fun createGroup(userId: Long, request: CreateGroupRequest): GroupResponse {
        val owner = userRepository.findById(userId).orElseThrow { UserNotFoundException("User not found") }
        val currency = currencyRepository.findByCode(request.currencyCode)
            ?: throw CurrencyNotFoundException("Currency not found: ${request.currencyCode}")

        val group = groupRepository.save(
            Group(
                name = request.name,
                description = request.description,
                owner = owner,
                currency = currency,
                status = groupStatus(GroupStatus.ACTIVE)
            )
        )

        groupMemberRepository.save(
            GroupMember(
                group = group,
                user = owner,
                role = groupRole(GroupRole.OWNER),
                status = memberStatus(MembershipStatus.ACTIVE),
                joinedAt = Instant.now()
            )
        )

        return group.toResponse(userId)
    }

    @Transactional(readOnly = true)
    fun getGroup(userId: Long, groupId: Long): GroupResponse {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        requireActiveMember(groupId, userId)
        return group.toResponse(userId)
    }

    @Transactional(readOnly = true)
    fun getMyGroups(userId: Long): List<GroupResponse> =
        groupMemberRepository.findActiveByUserId(userId).map { it.group.toResponse(userId) }

    @Transactional
    fun updateGroup(userId: Long, groupId: Long, request: UpdateGroupRequest): GroupResponse {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        val member = requireActiveMember(groupId, userId)
        requireActiveGroup(group)
        requireAdminOrOwner(member)

        group.name = request.name
        group.description = request.description
        return groupRepository.save(group).toResponse(userId)
    }

    @Transactional
    fun archiveGroup(userId: Long, groupId: Long) {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        val member = requireActiveMember(groupId, userId)
        requireActiveGroup(group)
        requireOwner(member)

        group.status = groupStatus(GroupStatus.ARCHIVED)
        group.archivedAt = Instant.now()
        groupRepository.save(group)
    }

    @Transactional(readOnly = true)
    fun getMembers(userId: Long, groupId: Long): List<GroupMemberResponse> {
        if (!groupRepository.existsById(groupId)) throw GroupNotFoundException("Group not found")
        requireActiveMember(groupId, userId)
        return groupMemberRepository.findActiveMembersByGroupId(groupId).map { it.toResponse() }
    }

    @Transactional
    fun removeMember(requesterId: Long, groupId: Long, targetUserId: Long) {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        requireActiveGroup(group)
        val requester = requireActiveMember(groupId, requesterId)

        val target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
            ?.takeIf { it.status.code == MembershipStatus.ACTIVE }
            ?: throw NotGroupMemberException("Target user is not an active member of this group")

        if (requesterId == targetUserId) {
            if (requester.role.code == GroupRole.OWNER)
                throw CannotRemoveOwnerException("Owner cannot leave the group; archive it instead")
            target.status = memberStatus(MembershipStatus.LEFT)
        } else {
            requireAdminOrOwner(requester)
            if (target.role.code == GroupRole.OWNER)
                throw CannotRemoveOwnerException("Cannot remove the owner from the group")
            if (requester.role.code == GroupRole.ADMIN && target.role.code == GroupRole.ADMIN)
                throw InsufficientPermissionsException("Admins cannot remove other admins")
            target.status = memberStatus(MembershipStatus.REMOVED)
        }

        target.leftAt = Instant.now()
        groupMemberRepository.save(target)
    }

    @Transactional
    fun updateMemberRole(requesterId: Long, groupId: Long, targetUserId: Long, request: UpdateMemberRoleRequest): GroupMemberResponse {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        val requester = requireActiveMember(groupId, requesterId)
        requireActiveGroup(group)
        requireOwner(requester)

        if (requesterId == targetUserId)
            throw InsufficientPermissionsException("Owner cannot change their own role")

        val newRoleCode = request.role.uppercase()
        if (newRoleCode !in setOf(GroupRole.ADMIN, GroupRole.MEMBER))
            throw InvalidInvitationException("Invalid role: must be ADMIN or MEMBER")

        val target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
            ?.takeIf { it.status.code == MembershipStatus.ACTIVE }
            ?: throw NotGroupMemberException("Target user is not an active member of this group")

        if (target.role.code == GroupRole.OWNER)
            throw CannotRemoveOwnerException("Cannot change the owner's role")

        target.role = groupRole(newRoleCode)
        return groupMemberRepository.save(target).toResponse()
    }

    @Transactional
    fun transferOwnership(requesterId: Long, groupId: Long, newOwnerId: Long): GroupMemberResponse {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        val requester = requireActiveMember(groupId, requesterId)
        requireActiveGroup(group)
        requireOwner(requester)

        if (requesterId == newOwnerId)
            throw InsufficientPermissionsException("You are already the owner")

        val newOwnerMember = groupMemberRepository.findByGroupIdAndUserId(groupId, newOwnerId)
            ?.takeIf { it.status.code == MembershipStatus.ACTIVE }
            ?: throw NotGroupMemberException("Target user is not an active member of this group")

        requester.role = groupRole(GroupRole.ADMIN)
        groupMemberRepository.save(requester)

        newOwnerMember.role = groupRole(GroupRole.OWNER)
        return groupMemberRepository.save(newOwnerMember).toResponse()
    }

    @Transactional
    fun createInvitation(userId: Long, groupId: Long, request: CreateInvitationRequest): GroupInvitationResponse {
        val group = groupRepository.findById(groupId).orElseThrow { GroupNotFoundException("Group not found") }
        val member = requireActiveMember(groupId, userId)
        requireActiveGroup(group)
        requireAdminOrOwner(member)

        val creator = userRepository.findById(userId).orElseThrow { UserNotFoundException("User not found") }
        val typeCode = request.type.uppercase()
        if (typeCode !in setOf(InvitationType.LINK, InvitationType.DIRECT))
            throw InvalidInvitationException("Invalid invitation type: must be LINK or DIRECT")

        var invitedUser: com.splittogether.backend.user.entity.User? = null
        if (typeCode == InvitationType.DIRECT) {
            val invitedUserId = request.invitedUserId
                ?: throw InvalidInvitationException("invitedUserId is required for DIRECT invitations")
            invitedUser = userRepository.findById(invitedUserId)
                .orElseThrow { UserNotFoundException("Invited user not found") }
            val alreadyMember = groupMemberRepository.findByGroupIdAndUserId(groupId, invitedUserId)
                ?.status?.code == MembershipStatus.ACTIVE
            if (alreadyMember) throw AlreadyGroupMemberException("User is already a member of this group")
        }

        val invitation = groupInvitationRepository.save(
            GroupInvitation(
                group = group,
                createdBy = creator,
                type = invType(typeCode),
                status = invStatus(InvitationStatus.PENDING),
                invitedUser = invitedUser,
                inviteCode = UUID.randomUUID().toString(),
                maxUses = if (typeCode == InvitationType.LINK) request.maxUses else 1,
                expiresAt = request.expiresAt
            )
        )

        // TODO: для DIRECT-приглашений нужно уведомить invitedUser (push или email)
        //  чтобы он узнал о приглашении и мог принять его через /join

        return invitation.toResponse(0L)
    }

    @Transactional(readOnly = true)
    fun getInvitations(userId: Long, groupId: Long): List<GroupInvitationResponse> {
        if (!groupRepository.existsById(groupId)) throw GroupNotFoundException("Group not found")
        val member = requireActiveMember(groupId, userId)
        requireAdminOrOwner(member)

        return groupInvitationRepository.findPendingByGroupId(groupId).map { invitation ->
            invitation.toResponse(invitationUseRepository.countByInvitationId(invitation.id))
        }
    }

    @Transactional
    fun revokeInvitation(userId: Long, groupId: Long, invitationId: Long) {
        if (!groupRepository.existsById(groupId)) throw GroupNotFoundException("Group not found")
        val member = requireActiveMember(groupId, userId)
        requireAdminOrOwner(member)

        val invitation = groupInvitationRepository.findById(invitationId)
            .orElseThrow { InvitationNotFoundException("Invitation not found") }

        if (invitation.group.id != groupId)
            throw InvitationNotFoundException("Invitation not found")
        if (invitation.status.code != InvitationStatus.PENDING)
            throw InvalidInvitationException("Invitation is no longer pending")

        invitation.status = invStatus(InvitationStatus.REVOKED)
        invitation.revokedAt = Instant.now()
        groupInvitationRepository.save(invitation)
    }

    @Transactional
    fun joinGroup(userId: Long, request: JoinGroupRequest): GroupResponse {
        val invitation = groupInvitationRepository.findByInviteCode(request.inviteCode)
            ?: throw InvalidInvitationException("Invalid invite code")

        if (invitation.status.code != InvitationStatus.PENDING)
            throw InvalidInvitationException("Invitation is no longer valid")

        if (invitation.expiresAt != null && Instant.now().isAfter(invitation.expiresAt))
            throw InvalidInvitationException("Invitation has expired")

        val group = invitation.group
        requireActiveGroup(group)

        if (invitation.type.code == InvitationType.DIRECT && invitation.invitedUser?.id != userId)
            throw InvalidInvitationException("This invitation is not for you")

        val usesCount = invitationUseRepository.countByInvitationId(invitation.id)
        val maxUses = invitation.maxUses
        if (maxUses != null && usesCount >= maxUses)
            throw InvalidInvitationException("Invitation has reached its maximum uses")

        addMemberAndRecordUse(userId, invitation)

        if (invitation.type.code == InvitationType.DIRECT) {
            invitation.status = invStatus(InvitationStatus.ACCEPTED)
            groupInvitationRepository.save(invitation)
        }

        return group.toResponse(userId)
    }

    @Transactional(readOnly = true)
    fun getMyInvitations(userId: Long): List<IncomingInvitationResponse> =
        groupInvitationRepository.findPendingDirectByInvitedUserId(userId, Instant.now()).map {
            IncomingInvitationResponse(
                id = it.id,
                groupId = it.group.id,
                groupName = it.group.name,
                groupCurrencyCode = it.group.currency.code,
                invitedById = it.createdBy.id,
                invitedByDisplayName = it.createdBy.displayName,
                expiresAt = it.expiresAt,
                createdAt = it.createdAt
            )
        }

    @Transactional
    fun acceptInvitation(userId: Long, invitationId: Long): GroupResponse {
        val invitation = groupInvitationRepository.findById(invitationId)
            .orElseThrow { InvitationNotFoundException("Invitation not found") }

        if (invitation.type.code != InvitationType.DIRECT)
            throw InvalidInvitationException("Only direct invitations can be accepted this way")
        if (invitation.invitedUser?.id != userId)
            throw InvalidInvitationException("This invitation is not for you")
        if (invitation.status.code != InvitationStatus.PENDING)
            throw InvalidInvitationException("Invitation is no longer pending")
        if (invitation.expiresAt != null && Instant.now().isAfter(invitation.expiresAt))
            throw InvalidInvitationException("Invitation has expired")

        requireActiveGroup(invitation.group)
        addMemberAndRecordUse(userId, invitation)
        invitation.status = invStatus(InvitationStatus.ACCEPTED)
        groupInvitationRepository.save(invitation)

        return invitation.group.toResponse(userId)
    }

    @Transactional
    fun rejectInvitation(userId: Long, invitationId: Long) {
        val invitation = groupInvitationRepository.findById(invitationId)
            .orElseThrow { InvitationNotFoundException("Invitation not found") }

        if (invitation.type.code != InvitationType.DIRECT)
            throw InvalidInvitationException("Only direct invitations can be rejected")
        if (invitation.invitedUser?.id != userId)
            throw InvalidInvitationException("This invitation is not for you")
        if (invitation.status.code != InvitationStatus.PENDING)
            throw InvalidInvitationException("Invitation is no longer pending")

        invitation.status = invStatus(InvitationStatus.DECLINED)
        groupInvitationRepository.save(invitation)
    }

    private fun addMemberAndRecordUse(userId: Long, invitation: GroupInvitation) {
        val group = invitation.group
        val existingMember = groupMemberRepository.findByGroupIdAndUserId(group.id, userId)
        if (existingMember?.status?.code == MembershipStatus.ACTIVE)
            throw AlreadyGroupMemberException("You are already a member of this group")

        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException("User not found") }

        if (existingMember != null) {
            existingMember.status = memberStatus(MembershipStatus.ACTIVE)
            existingMember.role = groupRole(GroupRole.MEMBER)
            existingMember.joinedAt = Instant.now()
            existingMember.leftAt = null
            groupMemberRepository.save(existingMember)
        } else {
            groupMemberRepository.save(
                GroupMember(
                    group = group,
                    user = user,
                    role = groupRole(GroupRole.MEMBER),
                    status = memberStatus(MembershipStatus.ACTIVE),
                    joinedAt = Instant.now()
                )
            )
        }

        invitationUseRepository.save(InvitationUse(invitation = invitation, user = user))
    }
}
