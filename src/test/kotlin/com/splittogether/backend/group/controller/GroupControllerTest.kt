package com.splittogether.backend.group.controller

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.LoginRequest
import com.splittogether.backend.auth.dto.RegisterRequest
import com.splittogether.backend.auth.dto.VerifyEmailRequest
import com.splittogether.backend.auth.repository.EmailVerificationRepository
import com.splittogether.backend.auth.service.AuthService
import com.splittogether.backend.group.dto.CreateGroupRequest
import com.splittogether.backend.group.dto.CreateInvitationRequest
import com.splittogether.backend.group.dto.JoinGroupRequest
import com.splittogether.backend.group.repository.*
import com.splittogether.backend.group.service.GroupService
import com.splittogether.backend.user.entity.User
import com.splittogether.backend.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class GroupControllerTest : AbstractIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var emailVerificationRepository: EmailVerificationRepository
    @Autowired private lateinit var groupRepository: GroupRepository
    @Autowired private lateinit var groupMemberRepository: GroupMemberRepository
    @Autowired private lateinit var groupInvitationRepository: GroupInvitationRepository
    @Autowired private lateinit var invitationUseRepository: InvitationUseRepository

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun registerAndVerify(email: String = "owner@test.com", password: String = "Password1!"): User {
        authService.register(RegisterRequest(email, password, "User"))
        val user = userRepository.findByEmail(email)!!
        val code = emailVerificationRepository.findLatestUnused(user, "REGISTRATION")!!.code
        authService.verifyEmail(VerifyEmailRequest(email, code))
        return user
    }

    private fun token(email: String = "owner@test.com", password: String = "Password1!"): String =
        authService.login(LoginRequest(email, password)).accessToken

    private fun createGroup(userId: Long): com.splittogether.backend.group.entity.Group {
        groupService.createGroup(userId, CreateGroupRequest("Test Group", null, "RUB"))
        return groupRepository.findAll().last()
    }

    private fun createLinkInviteCode(userId: Long, groupId: Long): String {
        val result = groupService.createInvitation(userId, groupId, CreateInvitationRequest("LINK"))
        return result.inviteCode!!
    }

    // ── POST /groups ──────────────────────────────────────────────────────────

    @Test
    fun `POST groups returns 201 for valid request`() {
        val user = registerAndVerify()
        val token = token()

        mockMvc.perform(
            post("/api/v1/groups")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"My Group","currencyCode":"RUB"}""")
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("My Group"))
            .andExpect(jsonPath("$.currencyCode").value("RUB"))
    }

    @Test
    fun `POST groups returns 400 for blank name`() {
        registerAndVerify()
        val token = token()

        mockMvc.perform(
            post("/api/v1/groups")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"","currencyCode":"RUB"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST groups returns 401 without token`() {
        mockMvc.perform(
            post("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Group","currencyCode":"RUB"}""")
        ).andExpect(status().isUnauthorized)
    }

    // ── GET /groups ───────────────────────────────────────────────────────────

    @Test
    fun `GET groups returns 200 with my groups`() {
        val user = registerAndVerify()
        val token = token()
        createGroup(user.id)

        mockMvc.perform(
            get("/api/v1/groups")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Test Group"))
    }

    @Test
    fun `GET groups returns 401 without token`() {
        mockMvc.perform(get("/api/v1/groups"))
            .andExpect(status().isUnauthorized)
    }

    // ── GET /groups/{id} ──────────────────────────────────────────────────────

    @Test
    fun `GET group returns 200 for member`() {
        val user = registerAndVerify()
        val token = token()
        val group = createGroup(user.id)

        mockMvc.perform(
            get("/api/v1/groups/${group.id}")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(group.id))
    }

    @Test
    fun `GET group returns 404 for unknown group`() {
        registerAndVerify()
        val token = token()

        mockMvc.perform(
            get("/api/v1/groups/999")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `GET group returns 403 for non-member`() {
        val owner = registerAndVerify()
        val other = registerAndVerify("other@test.com")
        val otherToken = token("other@test.com")
        val group = createGroup(owner.id)

        mockMvc.perform(
            get("/api/v1/groups/${group.id}")
                .header("Authorization", "Bearer $otherToken")
        ).andExpect(status().isForbidden)
    }

    // ── PUT /groups/{id} ──────────────────────────────────────────────────────

    @Test
    fun `PUT group returns 200 with updated data`() {
        val user = registerAndVerify()
        val token = token()
        val group = createGroup(user.id)

        mockMvc.perform(
            put("/api/v1/groups/${group.id}")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Updated Name"}""")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Name"))
    }

    @Test
    fun `PUT group returns 403 for regular member`() {
        val owner = registerAndVerify()
        val member = registerAndVerify("member@test.com")
        val memberToken = token("member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInviteCode(owner.id, group.id)))

        mockMvc.perform(
            put("/api/v1/groups/${group.id}")
                .header("Authorization", "Bearer $memberToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Hack"}""")
        ).andExpect(status().isForbidden)
    }

    // ── DELETE /groups/{id} ───────────────────────────────────────────────────

    @Test
    fun `DELETE group returns 204 for owner`() {
        val user = registerAndVerify()
        val token = token()
        val group = createGroup(user.id)

        mockMvc.perform(
            delete("/api/v1/groups/${group.id}")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE group returns 401 without token`() {
        mockMvc.perform(delete("/api/v1/groups/1"))
            .andExpect(status().isUnauthorized)
    }

    // ── GET /groups/{id}/members ──────────────────────────────────────────────

    @Test
    fun `GET members returns 200 with member list`() {
        val user = registerAndVerify()
        val token = token()
        val group = createGroup(user.id)

        mockMvc.perform(
            get("/api/v1/groups/${group.id}/members")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].role").value("OWNER"))
    }

    // ── DELETE /groups/{id}/members/{userId} ──────────────────────────────────

    @Test
    fun `DELETE member returns 204 when member leaves`() {
        val owner = registerAndVerify()
        val member = registerAndVerify("member@test.com")
        val memberToken = token("member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInviteCode(owner.id, group.id)))

        mockMvc.perform(
            delete("/api/v1/groups/${group.id}/members/${member.id}")
                .header("Authorization", "Bearer $memberToken")
        ).andExpect(status().isNoContent)
    }

    // ── PUT /groups/{id}/owner ────────────────────────────────────────────────

    @Test
    fun `PUT owner returns 200 and transfers ownership`() {
        val owner = registerAndVerify()
        val member = registerAndVerify("member@test.com")
        val token = token()
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInviteCode(owner.id, group.id)))

        mockMvc.perform(
            put("/api/v1/groups/${group.id}/owner")
                .header("Authorization", "Bearer $token")
                .param("newOwnerId", member.id.toString())
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("OWNER"))
    }

    @Test
    fun `PUT owner returns 403 for non-owner`() {
        val owner = registerAndVerify()
        val member = registerAndVerify("member@test.com")
        val memberToken = token("member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInviteCode(owner.id, group.id)))

        mockMvc.perform(
            put("/api/v1/groups/${group.id}/owner")
                .header("Authorization", "Bearer $memberToken")
                .param("newOwnerId", owner.id.toString())
        ).andExpect(status().isForbidden)
    }

    // ── POST /groups/{id}/invitations ─────────────────────────────────────────

    @Test
    fun `POST invitations returns 201 with invite code`() {
        val user = registerAndVerify()
        val token = token()
        val group = createGroup(user.id)

        mockMvc.perform(
            post("/api/v1/groups/${group.id}/invitations")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"LINK"}""")
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.inviteCode").exists())
    }

    @Test
    fun `POST invitations returns 403 for regular member`() {
        val owner = registerAndVerify()
        val member = registerAndVerify("member@test.com")
        val memberToken = token("member@test.com")
        val group = createGroup(owner.id)
        groupService.joinGroup(member.id, JoinGroupRequest(createLinkInviteCode(owner.id, group.id)))

        mockMvc.perform(
            post("/api/v1/groups/${group.id}/invitations")
                .header("Authorization", "Bearer $memberToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"LINK"}""")
        ).andExpect(status().isForbidden)
    }

    // ── POST /groups/join ─────────────────────────────────────────────────────

    @Test
    fun `POST join returns 200 and adds user to group`() {
        val owner = registerAndVerify()
        val joiner = registerAndVerify("joiner@test.com")
        val joinerToken = token("joiner@test.com")
        val group = createGroup(owner.id)
        val code = createLinkInviteCode(owner.id, group.id)

        mockMvc.perform(
            post("/api/v1/groups/join")
                .header("Authorization", "Bearer $joinerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"$code"}""")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(group.id))
    }

    @Test
    fun `POST join returns 400 for invalid invite code`() {
        registerAndVerify()
        val token = token()

        mockMvc.perform(
            post("/api/v1/groups/join")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"invalid-code"}""")
        ).andExpect(status().isBadRequest)
    }

    // ── GET /groups/{id}/invitations ──────────────────────────────────────────

    @Test
    fun `GET invitations returns 200 for owner`() {
        val user = registerAndVerify()
        val token = token()
        val group = createGroup(user.id)
        createLinkInviteCode(user.id, group.id)

        mockMvc.perform(
            get("/api/v1/groups/${group.id}/invitations")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].status").value("PENDING"))
    }

    // ── DELETE /groups/{id}/invitations/{invId} ───────────────────────────────

    @Test
    fun `DELETE invitation returns 204 for owner`() {
        val user = registerAndVerify()
        val token = token()
        val group = createGroup(user.id)
        val result = groupService.createInvitation(user.id, group.id, CreateInvitationRequest("LINK"))
        val inv = groupInvitationRepository.findAll().last()

        mockMvc.perform(
            delete("/api/v1/groups/${group.id}/invitations/${inv.id}")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)
    }
}
