package com.splittogether.backend.file

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.auth.dto.LoginRequest
import com.splittogether.backend.auth.service.AuthService
import com.splittogether.backend.file.repository.StoredFileRepository
import com.splittogether.backend.file.service.GroupFileService
import com.splittogether.backend.group.dto.CreateGroupRequest
import com.splittogether.backend.group.dto.CreateInvitationRequest
import com.splittogether.backend.group.dto.GroupResponse
import com.splittogether.backend.group.dto.JoinGroupRequest
import com.splittogether.backend.group.service.GroupService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GroupFileApiTest : AbstractIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var authService: AuthService
    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var groupFileService: GroupFileService
    @Autowired private lateinit var storedFileRepository: StoredFileRepository

    private fun token(email: String): String =
        authService.login(LoginRequest(email, generator.defaultPassword)).accessToken

    private fun doc(name: String = "report.pdf") =
        MockMultipartFile("file", name, "application/pdf", byteArrayOf(1, 2, 3, 4))

    private fun groupWithMember(): GroupResponse {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = groupService.createGroup(owner.id, CreateGroupRequest("G", null, "RUB"))
        val invite = groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("LINK"))
        groupService.joinGroup(member.id, JoinGroupRequest(invite.token!!))
        return group
    }

    private fun generatorUserId(email: String): Long =
        generator.userRepository.findByEmail(email)!!.id

    @Test
    fun `POST file by member returns 201 with metadata and url`() {
        val group = groupWithMember()

        mockMvc.perform(
            multipart("/api/v1/groups/${group.id}/files").file(doc())
                .param("description", "Q1 report")
                .header("Authorization", "Bearer ${token("member@test.com")}")
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.originalName").value("report.pdf"))
            .andExpect(jsonPath("$.description").value("Q1 report"))
            .andExpect(jsonPath("$.url").isNotEmpty)
    }

    @Test
    fun `GET list and single file return uploaded files`() {
        val group = groupWithMember()
        groupFileService.upload(generatorUserId("member@test.com"), group.id, doc(), null)

        mockMvc.perform(
            get("/api/v1/groups/${group.id}/files")
                .header("Authorization", "Bearer ${token("owner@test.com")}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].url").isNotEmpty)
    }

    @Test
    fun `DELETE by uploader soft-deletes the file`() {
        val group = groupWithMember()
        val file = groupFileService.upload(generatorUserId("member@test.com"), group.id, doc(), null)

        mockMvc.perform(
            delete("/api/v1/groups/${group.id}/files/${file.id}")
                .header("Authorization", "Bearer ${token("member@test.com")}")
        ).andExpect(status().isNoContent)

        assertNull(storedFileRepository.findActiveById(file.id))
        assertNotNull(storedFileRepository.findById(file.id).get().deletedAt)
    }

    @Test
    fun `DELETE others file by admin owner is allowed`() {
        val group = groupWithMember()
        val file = groupFileService.upload(generatorUserId("member@test.com"), group.id, doc(), null)

        mockMvc.perform(
            delete("/api/v1/groups/${group.id}/files/${file.id}")
                .header("Authorization", "Bearer ${token("owner@test.com")}")
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE others file by plain member returns 403`() {
        val owner = generator.user(email = "owner@test.com")
        val m1 = generator.user(email = "m1@test.com")
        val m2 = generator.user(email = "m2@test.com")
        val group = groupService.createGroup(owner.id, CreateGroupRequest("G", null, "RUB"))
        val invite = groupService.createInvitation(owner.id, group.id, CreateInvitationRequest("LINK", maxUses = 5))
        groupService.joinGroup(m1.id, JoinGroupRequest(invite.token!!))
        groupService.joinGroup(m2.id, JoinGroupRequest(invite.token!!))
        val file = groupFileService.upload(m1.id, group.id, doc(), null)

        mockMvc.perform(
            delete("/api/v1/groups/${group.id}/files/${file.id}")
                .header("Authorization", "Bearer ${token("m2@test.com")}")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `POST file by non-member returns 403`() {
        val owner = generator.user(email = "owner@test.com")
        generator.user(email = "outsider@test.com")
        val group = groupService.createGroup(owner.id, CreateGroupRequest("G", null, "RUB"))

        mockMvc.perform(
            multipart("/api/v1/groups/${group.id}/files").file(doc())
                .header("Authorization", "Bearer ${token("outsider@test.com")}")
        ).andExpect(status().isForbidden)
    }
}
