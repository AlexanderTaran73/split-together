package com.splittogether.backend.user.service

import com.splittogether.backend.balance.service.BalanceService
import com.splittogether.backend.common.exception.InvalidPrivacySettingException
import com.splittogether.backend.common.exception.UserNotFoundException
import com.splittogether.backend.friendship.service.FriendshipService
import com.splittogether.backend.group.dto.GroupResponse
import com.splittogether.backend.group.dto.IncomingInvitationResponse
import com.splittogether.backend.group.service.GroupService
import com.splittogether.backend.user.dto.UpdatePrivacyRequest
import com.splittogether.backend.user.dto.UpdateProfileRequest
import com.splittogether.backend.user.dto.UserBalanceResponse
import com.splittogether.backend.user.dto.UserPrivacyResponse
import com.splittogether.backend.user.dto.UserResponse
import com.splittogether.backend.user.entity.SearchVisibility
import com.splittogether.backend.user.entity.User
import com.splittogether.backend.storage.FileConstraints
import com.splittogether.backend.storage.service.AvatarUrlResolver
import com.splittogether.backend.storage.service.FileValidator
import com.splittogether.backend.storage.service.StorageService
import com.splittogether.backend.user.repository.GroupInvitePolicyRepository
import com.splittogether.backend.user.repository.SearchVisibilityRepository
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val groupService: GroupService,
    private val balanceService: BalanceService,
    private val friendshipService: FriendshipService,
    private val searchVisibilityRepository: SearchVisibilityRepository,
    private val groupInvitePolicyRepository: GroupInvitePolicyRepository,
    private val storageService: StorageService,
    private val fileValidator: FileValidator,
    private val avatarUrlResolver: AvatarUrlResolver
) {

    @Transactional(readOnly = true)
    fun getMe(userId: Long): UserResponse =
        userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }
            .toResponse()

    @Transactional(readOnly = true)
    fun search(requesterId: Long, query: String): List<UserResponse> {
        val friendIds = friendshipService.friendIds(requesterId).toHashSet()
        val blockedIds = friendshipService.blockedUserIds(requesterId).toHashSet()
        return userRepository.search(requesterId, query)
            .filter { it.id !in blockedIds && it.isVisibleTo(friendIds) }
            .map { it.toResponse() }
    }

    @Transactional
    fun updateMe(userId: Long, request: UpdateProfileRequest): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }
        user.displayName = request.displayName
        return userRepository.save(user).toResponse()
    }

    @Transactional
    fun updateAvatar(userId: Long, file: MultipartFile): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }
        fileValidator.validate(file, FileConstraints.AVATAR_MAX_BYTES) { it.startsWith("image/") }
        val key = "avatars/users/$userId/${UUID.randomUUID()}"
        storageService.upload(key, file.bytes, file.contentType)
        user.avatarObjectKey = key
        return userRepository.save(user).toResponse()
    }

    @Transactional
    fun deleteAvatar(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }
        user.avatarObjectKey = null
        userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun getPrivacy(userId: Long): UserPrivacyResponse =
        userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }
            .toPrivacyResponse()

    @Transactional
    fun updatePrivacy(userId: Long, request: UpdatePrivacyRequest): UserPrivacyResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }
        user.searchVisibility = searchVisibilityRepository.findByCode(request.searchVisibility.uppercase())
            ?: throw InvalidPrivacySettingException("Invalid search visibility: ${request.searchVisibility}")
        user.groupInvitePolicy = groupInvitePolicyRepository.findByCode(request.groupInvitePolicy.uppercase())
            ?: throw InvalidPrivacySettingException("Invalid group invite policy: ${request.groupInvitePolicy}")
        return userRepository.save(user).toPrivacyResponse()
    }

    @Transactional(readOnly = true)
    fun getMyGroups(userId: Long): List<GroupResponse> =
        groupService.getMyGroups(userId)

    @Transactional(readOnly = true)
    fun getMyBalance(userId: Long): UserBalanceResponse =
        balanceService.getUserBalance(userId)

    @Transactional(readOnly = true)
    fun getMyInvitations(userId: Long): List<IncomingInvitationResponse> =
        groupService.getMyInvitations(userId)

    private fun User.isVisibleTo(friendIds: Set<Long>): Boolean =
        when (searchVisibility.code) {
            SearchVisibility.EVERYONE -> true
            SearchVisibility.FRIENDS -> id in friendIds
            else -> false
        }

    private fun User.toResponse() = UserResponse(
        id = id,
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrlResolver.resolve(avatarObjectKey),
        createdAt = createdAt
    )

    private fun User.toPrivacyResponse() = UserPrivacyResponse(
        searchVisibility = searchVisibility.code,
        groupInvitePolicy = groupInvitePolicy.code
    )
}
