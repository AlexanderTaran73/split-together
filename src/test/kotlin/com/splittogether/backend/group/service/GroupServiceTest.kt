package com.splittogether.backend.group.service

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.RegisterRequest
import com.splittogether.backend.auth.service.AuthService
import com.splittogether.backend.common.exception.*
import com.splittogether.backend.group.dto.*
import com.splittogether.backend.group.entity.GroupInvitation
import com.splittogether.backend.group.repository.*
import com.splittogether.backend.user.entity.User
import com.splittogether.backend.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.*

class GroupServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var groupRepository: GroupRepository
    @Autowired private lateinit var groupMemberRepository: GroupMemberRepository
    @Autowired private lateinit var groupInvitationRepository: GroupInvitationRepository
    @Autowired private lateinit var invitationUseRepository: InvitationUseRepository

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun createUser(email: String = "user@test.com"): User {
        authService.register(RegisterRequest(email, "Password1!", "User"))
        return userRepository.findByEmail(email)!!
    }

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
        val owner = createUser()

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
        val owner = createUser()

        assertFailsWith<CurrencyNotFoundException> {
            groupService.createGroup(owner.id, CreateGroupRequest("Group", null, "XYZ"))
        }
    }

    // ── getGroup ──────────────────────────────────────────────────────────────

    @Test
    fun `getGroup returns group for active member`() {
        val owner = createUser()
        val group = createGroup(owner.id)

        val response = groupService.getGroup(owner.id, group.id)

        assertEquals(group.id, response.id)
        assertEquals("Test Group", response.name)
    }

    @Test
    fun `getGroup throws GroupNotFoundException for unknown group`() {
        val owner = createUser()

        assertFailsWith<GroupNotFoundException> {
            groupService.getGroup(owner.id, 999L)
        }
    }

    @Test
    fun `getGroup throws NotGroupMemberException for non-member`() {
        val owner = createUser()
        val other = createUser("other@test.com")
        val group = createGroup(owner.id)

        assertFailsWith<NotGroupMemberException> {
            groupService.getGroup(other.id, group.id)
        }
    }

    // ── getMyGroups ───────────────────────────────────────────────────────────

    @Test
    fun `getMyGroups returns all active groups for user`() {
        val owner = createUser()
        createGroup(owner.id, "Group 1")
        createGroup(owner.id, "Group 2")

        val result = groupService.getMyGroups(owner.id)

        assertEquals(2, result.size)
    }

    @Test
    fun `getMyGroups returns empty list when user has no groups`() {
        val user = createUser()

        val result = groupService.getMyGroups(user.id)

        assertTrue(result.isEmpty())
    }

    // ── updateGroup ───────────────────────────────────────────────────────────

    @Test
    fun `updateGroup updates name and description`() {
        val owner = createUser()
        val group = createGroup(owner.id)

        val response = groupService.updateGroup(owner.id, group.id, UpdateGroupRequest("New Name", "New desc"))

        assertEquals("New Name", response.name)
        assertEquals("New desc", response.description)
    }

    @Test
    fun `updateGroup throws InsufficientPermissionsException for regular member`() {
        val owner = createUser()
        val member = createUser("member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))

        assertFailsWith<InsufficientPermissionsException> {
            groupService.updateGroup(member.id, group.id, UpdateGroupRequest("Hack"))
        }
    }

    @Test
    fun `updateGroup throws GroupArchivedException for archived group`() {
        val owner = createUser()
        val group = createGroup(owner.id)
        groupService.archiveGroup(owner.id, group.id)

        assertFailsWith<GroupArchivedException> {
            groupService.updateGroup(owner.id, group.id, UpdateGroupRequest("New Name"))
        }
    }

    // ── archiveGroup ──────────────────────────────────────────────────────────

    @Test
    fun `archiveGroup sets group status to ARCHIVED`() {
        val owner = createUser()
        val group = createGroup(owner.id)

        groupService.archiveGroup(owner.id, group.id)

        assertEquals("ARCHIVED", groupRepository.findById(group.id).get().status.code)
    }

    @Test
    fun `archiveGroup throws InsufficientPermissionsException for admin`() {
        val owner = createUser()
        val admin = createUser("admin@test.com")
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
        val owner = createUser()
        val member = createUser("member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))

        val result = groupService.getMembers(owner.id, group.id)

        assertEquals(2, result.size)
    }

    // ── removeMember ──────────────────────────────────────────────────────────

    @Test
    fun `member can leave group`() {
        val owner = createUser()
        val member = createUser("member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))

        groupService.removeMember(member.id, group.id, member.id)

        val m = groupMemberRepository.findByGroupIdAndUserId(group.id, member.id)
        assertEquals("LEFT", m!!.status.code)
        assertNotNull(m.leftAt)
    }

    @Test
    fun `owner can remove a member`() {
        val owner = createUser()
        val member = createUser("member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))

        groupService.removeMember(owner.id, group.id, member.id)

        val m = groupMemberRepository.findByGroupIdAndUserId(group.id, member.id)
        assertEquals("REMOVED", m!!.status.code)
    }

    @Test
    fun `owner cannot leave the group`() {
        val owner = createUser()
        val group = createGroup(owner.id)

        assertFailsWith<CannotRemoveOwnerException> {
            groupService.removeMember(owner.id, group.id, owner.id)
        }
    }

    @Test
    fun `admin cannot remove another admin`() {
        val owner = createUser()
        val admin1 = createUser("admin1@test.com")
        val admin2 = createUser("admin2@test.com")
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
        val owner = createUser()
        val member = createUser("member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))

        groupService.transferOwnership(owner.id, group.id, member.id)

        assertEquals("OWNER", groupMemberRepository.findByGroupIdAndUserId(group.id, member.id)!!.role.code)
        assertEquals("ADMIN", groupMemberRepository.findByGroupIdAndUserId(group.id, owner.id)!!.role.code)
    }

    @Test
    fun `transferOwnership throws InsufficientPermissionsException for non-owner`() {
        val owner = createUser()
        val admin = createUser("admin@test.com")
        val member = createUser("member@test.com")
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
        val owner = createUser()
        val nonMember = createUser("nonmember@test.com")
        val group = createGroup(owner.id)

        assertFailsWith<NotGroupMemberException> {
            groupService.transferOwnership(owner.id, group.id, nonMember.id)
        }
    }

    // ── updateMemberRole ──────────────────────────────────────────────────────

    @Test
    fun `owner can promote member to admin`() {
        val owner = createUser()
        val member = createUser("member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))

        val result = groupService.updateMemberRole(owner.id, group.id, member.id, UpdateMemberRoleRequest("ADMIN"))

        assertEquals("ADMIN", result.role)
    }

    @Test
    fun `owner can demote admin to member`() {
        val owner = createUser()
        val admin = createUser("admin@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(admin.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))
        groupService.updateMemberRole(owner.id, group.id, admin.id, UpdateMemberRoleRequest("ADMIN"))

        val result = groupService.updateMemberRole(owner.id, group.id, admin.id, UpdateMemberRoleRequest("MEMBER"))

        assertEquals("MEMBER", result.role)
    }

    @Test
    fun `non-owner cannot change member roles`() {
        val owner = createUser()
        val admin = createUser("admin@test.com")
        val member = createUser("member@test.com")
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
        val owner = createUser()
        val group = createGroup(owner.id)

        val result = groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("LINK"))

        assertEquals("LINK", result.type)
        assertEquals("PENDING", result.status)
        assertNotNull(result.inviteCode)
    }

    @Test
    fun `createInvitation creates DIRECT invitation for specific user`() {
        val owner = createUser()
        val target = createUser("target@test.com")
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
        val owner = createUser()
        val member = createUser("member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInvitation(owner.id, group.id).inviteCode!!))

        assertFailsWith<InsufficientPermissionsException> {
            groupService.createInvitation(member.id, group.id, CreateInvitationRequest("LINK"))
        }
    }

    // ── joinGroup ─────────────────────────────────────────────────────────────

    @Test
    fun `joinGroup adds user as MEMBER with ACTIVE status`() {
        val owner = createUser()
        val joiner = createUser("joiner@test.com")
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
        val owner = createUser()
        val group = createGroup(owner.id)
        val inv1 = createLinkInvitation(owner.id, group.id)
        val joiner = createUser("joiner@test.com")
        groupService.joinGroup(joiner.id, JoinGroupRequest(inv1.inviteCode!!))
        val inv2 = createLinkInvitation(owner.id, group.id)

        assertFailsWith<AlreadyGroupMemberException> {
            groupService.joinGroup(joiner.id, JoinGroupRequest(inv2.inviteCode!!))
        }
    }

    @Test
    fun `joinGroup throws InvalidInvitationException for expired invitation`() {
        val owner = createUser()
        val group = createGroup(owner.id)
        groupService.createInvitation(
            owner.id, group.id,
            CreateInvitationRequest("LINK", expiresAt = Instant.now().minus(1, ChronoUnit.HOURS))
        )
        val inv = groupInvitationRepository.findAll().last()
        val joiner = createUser("joiner@test.com")

        assertFailsWith<InvalidInvitationException> {
            groupService.joinGroup(joiner.id, JoinGroupRequest(inv.inviteCode!!))
        }
    }

    @Test
    fun `joinGroup throws InvalidInvitationException when max uses reached`() {
        val owner = createUser()
        val group = createGroup(owner.id)
        groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("LINK", maxUses = 1))
        val inv = groupInvitationRepository.findAll().last()
        val joiner1 = createUser("joiner1@test.com")
        val joiner2 = createUser("joiner2@test.com")
        groupService.joinGroup(joiner1.id, JoinGroupRequest(inv.inviteCode!!))

        assertFailsWith<InvalidInvitationException> {
            groupService.joinGroup(joiner2.id, JoinGroupRequest(inv.inviteCode!!))
        }
    }

    @Test
    fun `joinGroup throws InvalidInvitationException when wrong user uses DIRECT invitation`() {
        val owner = createUser()
        val target = createUser("target@test.com")
        val other = createUser("other@test.com")
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
        val owner = createUser()
        val group = createGroup(owner.id)
        val inv = createLinkInvitation(owner.id, group.id)

        groupService.revokeInvitation(owner.id, group.id, inv.id)

        assertEquals("REVOKED", groupInvitationRepository.findById(inv.id).get().status.code)
    }

    @Test
    fun `member cannot revoke invitation`() {
        val owner = createUser()
        val member = createUser("member@test.com")
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
        val owner = createUser()
        val group = createGroup(owner.id)
        createLinkInvitation(owner.id, group.id)
        createLinkInvitation(owner.id, group.id)

        val result = groupService.getInvitations(owner.id, group.id)

        assertEquals(2, result.size)
        assertTrue(result.all { it.status == "PENDING" })
    }
}
