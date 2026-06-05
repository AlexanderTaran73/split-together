package com.splittogether.backend.group.service

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.common.exception.*
import com.splittogether.backend.group.dto.*
import com.splittogether.backend.group.entity.GroupInvitation
import com.splittogether.backend.group.repository.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.*

class GroupServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var groupRepository: GroupRepository
    @Autowired private lateinit var groupMemberRepository: GroupMemberRepository
    @Autowired private lateinit var groupInvitationRepository: GroupInvitationRepository
    @Autowired private lateinit var invitationUseRepository: InvitationUseRepository

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun createGroup(ownerId: Long, name: String = "Test Group"): com.splittogether.backend.group.entity.Group {
        groupService.createGroup(ownerId, CreateGroupRequest(name, null, "RUB"))
        return groupRepository.findAll().last()
    }

    private fun createLinkInvitation(userId: Long, groupId: Long): GroupInvitation {
        groupService.createInvitation(userId, groupId, CreateInvitationRequest("LINK"))
        return groupInvitationRepository.findAll().last()
    }

    // ── createGroup ───────────────────────────────────────────────────────────

    @Test
    fun `createGroup creates group and owner becomes ACTIVE member with OWNER role`() {
        val owner = generator.user()

        val response = groupService.createGroup(owner.id, CreateGroupRequest("My Group", null, "RUB"))

        assertEquals("My Group", response.name)
        assertEquals("RUB", response.currencyCode)
        assertEquals("ACTIVE", response.status)
        assertEquals(1L, response.memberCount)

        val member = groupMemberRepository.findByGroupIdAndUserId(response.id, owner.id)
        assertNotNull(member)
        assertEquals("OWNER", member!!.role.code)
        assertEquals("ACTIVE", member.status.code)
        assertNotNull(member.joinedAt)
    }

    @Test
    fun `createGroup throws CurrencyNotFoundException for unknown currency`() {
        val owner = generator.user()

        assertFailsWith<CurrencyNotFoundException> {
            groupService.createGroup(owner.id, CreateGroupRequest("Group", null, "XYZ"))
        }
    }

    // ── getGroup ──────────────────────────────────────────────────────────────

    @Test
    fun `getGroup returns group for active member`() {
        val owner = generator.user()
        val group = createGroup(owner.id)

        val response = groupService.getGroup(owner.id, group.id)

        assertEquals(group.id, response.id)
        assertEquals("Test Group", response.name)
    }

    @Test
    fun `getGroup throws GroupNotFoundException for unknown group`() {
        val owner = generator.user()

        assertFailsWith<GroupNotFoundException> {
            groupService.getGroup(owner.id, 999L)
        }
    }

    @Test
    fun `getGroup throws NotGroupMemberException for non-member`() {
        val owner = generator.user()
        val other = generator.user(email = "other@test.com")
        val group = createGroup(owner.id)

        assertFailsWith<NotGroupMemberException> {
            groupService.getGroup(other.id, group.id)
        }
    }

    // ── getMyGroups ───────────────────────────────────────────────────────────

    @Test
    fun `getMyGroups returns all active groups for user`() {
        val owner = generator.user()
        createGroup(owner.id, "Group 1")
        createGroup(owner.id, "Group 2")

        val result = groupService.getMyGroups(owner.id)

        assertEquals(2, result.size)
    }

    @Test
    fun `getMyGroups returns empty list when user has no groups`() {
        val user = generator.user()

        val result = groupService.getMyGroups(user.id)

        assertTrue(result.isEmpty())
    }

    // ── updateGroup ───────────────────────────────────────────────────────────

    @Test
    fun `updateGroup updates name and description`() {
        val owner = generator.user()
        val group = createGroup(owner.id)

        val response = groupService.updateGroup(owner.id, group.id, UpdateGroupRequest("New Name", "New desc"))

        assertEquals("New Name", response.name)
        assertEquals("New desc", response.description)
    }

    @Test
    fun `updateGroup throws InsufficientPermissionsException for regular member`() {
        val owner = generator.user()
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))

        assertFailsWith<InsufficientPermissionsException> {
            groupService.updateGroup(member.id, group.id, UpdateGroupRequest("Hack"))
        }
    }

    @Test
    fun `updateGroup throws GroupArchivedException for archived group`() {
        val owner = generator.user()
        val group = createGroup(owner.id)
        groupService.archiveGroup(owner.id, group.id)

        assertFailsWith<GroupArchivedException> {
            groupService.updateGroup(owner.id, group.id, UpdateGroupRequest("New Name"))
        }
    }

    // ── archiveGroup ──────────────────────────────────────────────────────────

    @Test
    fun `archiveGroup sets group status to ARCHIVED`() {
        val owner = generator.user()
        val group = createGroup(owner.id)

        groupService.archiveGroup(owner.id, group.id)

        assertEquals("ARCHIVED", groupRepository.findById(group.id).get().status.code)
    }

    @Test
    fun `archiveGroup throws InsufficientPermissionsException for admin`() {
        val owner = generator.user()
        val admin = generator.user(email = "admin@test.com")
        val group = createGroup(owner.id)
        val inv = createLinkInvitation(owner.id, group.id)
        groupService.joinGroup(admin.id, JoinGroupRequest(inv.inviteCode!!))
        groupService.updateMemberRole(owner.id, group.id, admin.id, UpdateMemberRoleRequest("ADMIN"))

        assertFailsWith<InsufficientPermissionsException> {
            groupService.archiveGroup(admin.id, group.id)
        }
    }

    // ── getMembers ────────────────────────────────────────────────────────────

    @Test
    fun `getMembers returns all active members`() {
        val owner = generator.user()
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))

        val result = groupService.getMembers(owner.id, group.id)

        assertEquals(2, result.size)
    }

    // ── removeMember ──────────────────────────────────────────────────────────

    @Test
    fun `member can leave group`() {
        val owner = generator.user()
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))

        groupService.removeMember(member.id, group.id, member.id)

        val m = groupMemberRepository.findByGroupIdAndUserId(group.id, member.id)
        assertEquals("LEFT", m!!.status.code)
        assertNotNull(m.leftAt)
    }

    @Test
    fun `owner can remove a member`() {
        val owner = generator.user()
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))

        groupService.removeMember(owner.id, group.id, member.id)

        val m = groupMemberRepository.findByGroupIdAndUserId(group.id, member.id)
        assertEquals("REMOVED", m!!.status.code)
    }

    @Test
    fun `owner cannot leave the group`() {
        val owner = generator.user()
        val group = createGroup(owner.id)

        assertFailsWith<CannotRemoveOwnerException> {
            groupService.removeMember(owner.id, group.id, owner.id)
        }
    }

    @Test
    fun `admin cannot remove another admin`() {
        val owner = generator.user()
        val admin1 = generator.user(email = "admin1@test.com")
        val admin2 = generator.user(email = "admin2@test.com")
        val group = createGroup(owner.id)
        val inv1 = createLinkInvitation(owner.id, group.id)
        groupService.joinGroup(admin1.id, JoinGroupRequest(inv1.inviteCode!!))
        groupService.updateMemberRole(owner.id, group.id, admin1.id, UpdateMemberRoleRequest("ADMIN"))
        val inv2 = createLinkInvitation(owner.id, group.id)
        groupService.joinGroup(admin2.id, JoinGroupRequest(inv2.inviteCode!!))
        groupService.updateMemberRole(owner.id, group.id, admin2.id, UpdateMemberRoleRequest("ADMIN"))

        assertFailsWith<InsufficientPermissionsException> {
            groupService.removeMember(admin1.id, group.id, admin2.id)
        }
    }

    // ── transferOwnership ─────────────────────────────────────────────────────

    @Test
    fun `transferOwnership makes target OWNER and demotes requester to ADMIN`() {
        val owner = generator.user()
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))

        groupService.transferOwnership(owner.id, group.id, member.id)

        assertEquals("OWNER", groupMemberRepository.findByGroupIdAndUserId(group.id, member.id)!!.role.code)
        assertEquals("ADMIN", groupMemberRepository.findByGroupIdAndUserId(group.id, owner.id)!!.role.code)
    }

    @Test
    fun `transferOwnership throws InsufficientPermissionsException for non-owner`() {
        val owner = generator.user()
        val admin = generator.user(email = "admin@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        val inv1 = createLinkInvitation(owner.id, group.id)
        groupService.joinGroup(admin.id, JoinGroupRequest(inv1.inviteCode!!))
        groupService.updateMemberRole(owner.id, group.id, admin.id, UpdateMemberRoleRequest("ADMIN"))
        val inv2 = createLinkInvitation(owner.id, group.id)
        groupService.joinGroup(member.id, JoinGroupRequest(inv2.inviteCode!!))

        assertFailsWith<InsufficientPermissionsException> {
            groupService.transferOwnership(admin.id, group.id, member.id)
        }
    }

    @Test
    fun `transferOwnership throws NotGroupMemberException for non-member target`() {
        val owner = generator.user()
        val nonMember = generator.user(email = "nonmember@test.com")
        val group = createGroup(owner.id)

        assertFailsWith<NotGroupMemberException> {
            groupService.transferOwnership(owner.id, group.id, nonMember.id)
        }
    }

    // ── updateMemberRole ──────────────────────────────────────────────────────

    @Test
    fun `owner can promote member to admin`() {
        val owner = generator.user()
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))

        val result = groupService.updateMemberRole(owner.id, group.id, member.id, UpdateMemberRoleRequest("ADMIN"))

        assertEquals("ADMIN", result.role)
    }

    @Test
    fun `owner can demote admin to member`() {
        val owner = generator.user()
        val admin = generator.user(email = "admin@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(admin.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))
        groupService.updateMemberRole(owner.id, group.id, admin.id, UpdateMemberRoleRequest("ADMIN"))

        val result = groupService.updateMemberRole(owner.id, group.id, admin.id, UpdateMemberRoleRequest("MEMBER"))

        assertEquals("MEMBER", result.role)
    }

    @Test
    fun `non-owner cannot change member roles`() {
        val owner = generator.user()
        val admin = generator.user(email = "admin@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        val inv1 = createLinkInvitation(owner.id, group.id)
        groupService.joinGroup(admin.id, JoinGroupRequest(inv1.inviteCode!!))
        groupService.updateMemberRole(owner.id, group.id, admin.id, UpdateMemberRoleRequest("ADMIN"))
        val inv2 = createLinkInvitation(owner.id, group.id)
        groupService.joinGroup(member.id, JoinGroupRequest(inv2.inviteCode!!))

        assertFailsWith<InsufficientPermissionsException> {
            groupService.updateMemberRole(admin.id, group.id, member.id, UpdateMemberRoleRequest("ADMIN"))
        }
    }

    // ── createInvitation ──────────────────────────────────────────────────────

    @Test
    fun `createInvitation creates LINK invitation with invite code`() {
        val owner = generator.user()
        val group = createGroup(owner.id)

        val result = groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("LINK"))

        assertEquals("LINK", result.type)
        assertEquals("PENDING", result.status)
        assertNotNull(result.inviteCode)
    }

    @Test
    fun `createInvitation creates DIRECT invitation for specific user`() {
        val owner = generator.user()
        val target = generator.user(email = "target@test.com")
        val group = createGroup(owner.id)

        val result = groupService.createInvitation(
            owner.id, group.id,
            CreateInvitationRequest("DIRECT", invitedUserId = target.id)
        )

        assertEquals("DIRECT", result.type)
        assertEquals(target.id, result.invitedUserId)
        assertNotNull(result.inviteCode)
    }

    @Test
    fun `member cannot create invitation`() {
        val owner = generator.user()
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))

        assertFailsWith<InsufficientPermissionsException> {
            groupService.createInvitation(member.id, group.id, CreateInvitationRequest("LINK"))
        }
    }

    // ── joinGroup ─────────────────────────────────────────────────────────────

    @Test
    fun `joinGroup adds user as MEMBER with ACTIVE status`() {
        val owner = generator.user()
        val joiner = generator.user(email = "joiner@test.com")
        val group = createGroup(owner.id)
        val inv = createLinkInvitation(owner.id, group.id)

        val result = groupService.joinGroup(joiner.id, JoinGroupRequest(inv.inviteCode!!))

        assertEquals(group.id, result.id)
        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, joiner.id)
        assertNotNull(member)
        assertEquals("MEMBER", member!!.role.code)
        assertEquals("ACTIVE", member.status.code)
    }

    @Test
    fun `joinGroup throws AlreadyGroupMemberException if already a member`() {
        val owner = generator.user()
        val group = createGroup(owner.id)
        val inv1 = createLinkInvitation(owner.id, group.id)
        val joiner = generator.user(email = "joiner@test.com")
        groupService.joinGroup(joiner.id, JoinGroupRequest(inv1.inviteCode!!))
        val inv2 = createLinkInvitation(owner.id, group.id)

        assertFailsWith<AlreadyGroupMemberException> {
            groupService.joinGroup(joiner.id, JoinGroupRequest(inv2.inviteCode!!))
        }
    }

    @Test
    fun `joinGroup throws InvalidInvitationException for expired invitation`() {
        val owner = generator.user()
        val group = createGroup(owner.id)
        groupService.createInvitation(
            owner.id, group.id,
            CreateInvitationRequest("LINK", expiresAt = Instant.now().minus(1, ChronoUnit.HOURS))
        )
        val inv = groupInvitationRepository.findAll().last()
        val joiner = generator.user(email = "joiner@test.com")

        assertFailsWith<InvalidInvitationException> {
            groupService.joinGroup(joiner.id, JoinGroupRequest(inv.inviteCode!!))
        }
    }

    @Test
    fun `joinGroup throws InvalidInvitationException when max uses reached`() {
        val owner = generator.user()
        val group = createGroup(owner.id)
        groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("LINK", maxUses = 1))
        val inv = groupInvitationRepository.findAll().last()
        val joiner1 = generator.user(email = "joiner1@test.com")
        val joiner2 = generator.user(email = "joiner2@test.com")
        groupService.joinGroup(joiner1.id, JoinGroupRequest(inv.inviteCode!!))

        assertFailsWith<InvalidInvitationException> {
            groupService.joinGroup(joiner2.id, JoinGroupRequest(inv.inviteCode!!))
        }
    }

    @Test
    fun `joinGroup throws InvalidInvitationException when wrong user uses DIRECT invitation`() {
        val owner = generator.user()
        val target = generator.user(email = "target@test.com")
        val other = generator.user(email = "other@test.com")
        val group = createGroup(owner.id)
        val result = groupService.createInvitation(
            owner.id, group.id,
            CreateInvitationRequest("DIRECT", invitedUserId = target.id)
        )
        val inv = groupInvitationRepository.findByInviteCode(result.inviteCode!!)!!

        assertFailsWith<InvalidInvitationException> {
            groupService.joinGroup(other.id, JoinGroupRequest(inv.inviteCode!!))
        }
    }

    // ── revokeInvitation ──────────────────────────────────────────────────────

    @Test
    fun `revokeInvitation sets status to REVOKED`() {
        val owner = generator.user()
        val group = createGroup(owner.id)
        val inv = createLinkInvitation(owner.id, group.id)

        groupService.revokeInvitation(owner.id, group.id, inv.id)

        assertEquals("REVOKED", groupInvitationRepository.findById(inv.id).get().status.code)
    }

    @Test
    fun `member cannot revoke invitation`() {
        val owner = generator.user()
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id)
        val inv = createLinkInvitation(owner.id, group.id)
        groupService.joinGroup(member.id, JoinGroupRequest(inv.inviteCode!!))
        val inv2 = createLinkInvitation(owner.id, group.id)

        assertFailsWith<InsufficientPermissionsException> {
            groupService.revokeInvitation(member.id, group.id, inv2.id)
        }
    }

    // ── getInvitations ────────────────────────────────────────────────────────

    @Test
    fun `getInvitations returns PENDING invitations with use counts`() {
        val owner = generator.user()
        val group = createGroup(owner.id)
        createLinkInvitation(owner.id, group.id)
        createLinkInvitation(owner.id, group.id)

        val result = groupService.getInvitations(owner.id, group.id)

        assertEquals(2, result.size)
        assertTrue(result.all { it.status == "PENDING" })
    }

    // ── getMyInvitations ──────────────────────────────────────────────────────

    @Test
    fun `getMyInvitations returns pending DIRECT invitations for user`() {
        val owner = generator.user()
        val invited = generator.user(email = "invited@test.com")
        val group = createGroup(owner.id)
        groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("DIRECT", invitedUserId = invited.id))

        val result = groupService.getMyInvitations(invited.id)

        assertEquals(1, result.size)
        assertEquals(group.id, result[0].groupId)
        assertEquals(owner.id, result[0].invitedById)
    }

    @Test
    fun `getMyInvitations does not return LINK invitations`() {
        val owner = generator.user()
        val group = createGroup(owner.id)
        createLinkInvitation(owner.id, group.id)

        val result = groupService.getMyInvitations(owner.id)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getMyInvitations does not return invitations meant for other users`() {
        val owner = generator.user()
        val invited = generator.user(email = "invited@test.com")
        val other = generator.user(email = "other@test.com")
        val group = createGroup(owner.id)
        groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("DIRECT", invitedUserId = invited.id))

        val result = groupService.getMyInvitations(other.id)

        assertTrue(result.isEmpty())
    }

    // ── acceptInvitation ──────────────────────────────────────────────────────

    @Test
    fun `acceptInvitation adds user to group and sets status to ACCEPTED`() {
        val owner = generator.user()
        val invited = generator.user(email = "invited@test.com")
        val group = createGroup(owner.id)
        groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("DIRECT", invitedUserId = invited.id))
        val inv = groupInvitationRepository.findAll().last()

        groupService.acceptInvitation(invited.id, inv.id)

        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, invited.id)
        assertNotNull(member)
        assertEquals("ACTIVE", member!!.status.code)
        assertEquals("ACCEPTED", groupInvitationRepository.findById(inv.id).get().status.code)
    }

    @Test
    fun `acceptInvitation throws InvalidInvitationException for wrong user`() {
        val owner = generator.user()
        val invited = generator.user(email = "invited@test.com")
        val other = generator.user(email = "other@test.com")
        val group = createGroup(owner.id)
        groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("DIRECT", invitedUserId = invited.id))
        val inv = groupInvitationRepository.findAll().last()

        assertFailsWith<InvalidInvitationException> {
            groupService.acceptInvitation(other.id, inv.id)
        }
    }

    @Test
    fun `acceptInvitation throws InvalidInvitationException for expired invitation`() {
        val owner = generator.user()
        val invited = generator.user(email = "invited@test.com")
        val group = createGroup(owner.id)
        groupService.createInvitation(
            owner.id, group.id,
            CreateInvitationRequest("DIRECT", invitedUserId = invited.id, expiresAt = Instant.now().minus(1, ChronoUnit.HOURS))
        )
        val inv = groupInvitationRepository.findAll().last()

        assertFailsWith<InvalidInvitationException> {
            groupService.acceptInvitation(invited.id, inv.id)
        }
    }

    @Test
    fun `acceptInvitation throws InvalidInvitationException for LINK invitation`() {
        val owner = generator.user()
        val other = generator.user(email = "other@test.com")
        val group = createGroup(owner.id)
        val inv = createLinkInvitation(owner.id, group.id)

        assertFailsWith<InvalidInvitationException> {
            groupService.acceptInvitation(other.id, inv.id)
        }
    }

    // ── rejectInvitation ──────────────────────────────────────────────────────

    @Test
    fun `rejectInvitation sets status to DECLINED`() {
        val owner = generator.user()
        val invited = generator.user(email = "invited@test.com")
        val group = createGroup(owner.id)
        groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("DIRECT", invitedUserId = invited.id))
        val inv = groupInvitationRepository.findAll().last()

        groupService.rejectInvitation(invited.id, inv.id)

        assertEquals("DECLINED", groupInvitationRepository.findById(inv.id).get().status.code)
    }

    @Test
    fun `rejectInvitation throws InvalidInvitationException for wrong user`() {
        val owner = generator.user()
        val invited = generator.user(email = "invited@test.com")
        val other = generator.user(email = "other@test.com")
        val group = createGroup(owner.id)
        groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("DIRECT", invitedUserId = invited.id))
        val inv = groupInvitationRepository.findAll().last()

        assertFailsWith<InvalidInvitationException> {
            groupService.rejectInvitation(other.id, inv.id)
        }
    }
}
