package com.splittogether.backend.file.service

import com.splittogether.backend.common.exception.GroupNotFoundException
import com.splittogether.backend.file.dto.FileResponse
import com.splittogether.backend.file.entity.FileOwnerType
import com.splittogether.backend.group.entity.GroupRole
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.group.service.MembershipGuard
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class GroupFileService(
    private val fileService: FileService,
    private val membershipGuard: MembershipGuard,
    private val groupRepository: GroupRepository
) {

    fun upload(userId: Long, groupId: Long, file: MultipartFile, description: String?): FileResponse {
        requireGroupExists(groupId)
        membershipGuard.requireActiveMember(groupId, userId)
        return fileService.upload(FileOwnerType.GROUP, groupId, userId, file, description, "groups/$groupId/files")
    }

    fun list(userId: Long, groupId: Long): List<FileResponse> {
        requireGroupExists(groupId)
        membershipGuard.requireActiveMember(groupId, userId)
        return fileService.list(FileOwnerType.GROUP, groupId)
    }

    fun get(userId: Long, groupId: Long, fileId: Long): FileResponse {
        requireGroupExists(groupId)
        membershipGuard.requireActiveMember(groupId, userId)
        return fileService.get(FileOwnerType.GROUP, groupId, fileId)
    }

    fun delete(userId: Long, groupId: Long, fileId: Long) {
        requireGroupExists(groupId)
        val member = membershipGuard.requireActiveMember(groupId, userId)
        val canManage = member.role.code != GroupRole.MEMBER
        fileService.softDelete(FileOwnerType.GROUP, groupId, fileId, userId, canManage)
    }

    private fun requireGroupExists(groupId: Long) {
        if (!groupRepository.existsById(groupId)) throw GroupNotFoundException("Group not found")
    }
}
