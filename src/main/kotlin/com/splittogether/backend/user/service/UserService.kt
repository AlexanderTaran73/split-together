package com.splittogether.backend.user.service

import com.splittogether.backend.balance.service.BalanceService
import com.splittogether.backend.common.exception.UserNotFoundException
import com.splittogether.backend.group.dto.GroupResponse
import com.splittogether.backend.group.dto.IncomingInvitationResponse
import com.splittogether.backend.group.service.GroupService
import com.splittogether.backend.user.dto.UpdateProfileRequest
import com.splittogether.backend.user.dto.UserBalanceResponse
import com.splittogether.backend.user.dto.UserResponse
import com.splittogether.backend.user.entity.User
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val groupService: GroupService,
    private val balanceService: BalanceService
) {

    @Transactional(readOnly = true)
    fun getMe(userId: Long): UserResponse =
        userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }
            .toResponse()

    @Transactional(readOnly = true)
    fun search(query: String): List<UserResponse> =
        userRepository.search(query).map { it.toResponse() }

    @Transactional
    fun updateMe(userId: Long, request: UpdateProfileRequest): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }
        user.displayName = request.displayName
        return userRepository.save(user).toResponse()
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

    private fun User.toResponse() = UserResponse(
        id = id,
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl,
        createdAt = createdAt
    )
}
