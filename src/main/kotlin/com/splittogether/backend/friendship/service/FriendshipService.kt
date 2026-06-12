package com.splittogether.backend.friendship.service

import com.splittogether.backend.common.exception.AlreadyFriendsException
import com.splittogether.backend.common.exception.FriendshipNotFoundException
import com.splittogether.backend.common.exception.InvalidFriendRequestException
import com.splittogether.backend.common.exception.UserBlockedException
import com.splittogether.backend.common.exception.UserNotFoundException
import com.splittogether.backend.friendship.dto.FriendRequestResponse
import com.splittogether.backend.friendship.dto.FriendResponse
import com.splittogether.backend.friendship.entity.Friendship
import com.splittogether.backend.friendship.entity.FriendshipStatus
import com.splittogether.backend.friendship.repository.FriendshipRepository
import com.splittogether.backend.friendship.repository.FriendshipStatusRepository
import com.splittogether.backend.user.entity.User
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class FriendshipService(
    private val friendshipRepository: FriendshipRepository,
    private val friendshipStatusRepository: FriendshipStatusRepository,
    private val userRepository: UserRepository
) {

    private fun status(code: String): FriendshipStatus =
        friendshipStatusRepository.findByCode(code) ?: error("Missing reference data: friendship_status=$code")

    fun areFriends(a: Long, b: Long): Boolean = friendshipRepository.areFriends(a, b)

    fun isBlockedBetween(a: Long, b: Long): Boolean = friendshipRepository.isBlockedBetween(a, b)

    fun friendIds(userId: Long): List<Long> = friendshipRepository.findFriendIds(userId)

    fun blockedUserIds(userId: Long): List<Long> = friendshipRepository.findBlockedUserIds(userId)

    @Transactional
    fun sendRequest(requesterId: Long, addresseeId: Long): FriendRequestResponse {
        if (requesterId == addresseeId)
            throw InvalidFriendRequestException("Cannot send a friend request to yourself")

        val addressee = userRepository.findById(addresseeId)
            .orElseThrow { UserNotFoundException("User not found") }

        val existing = friendshipRepository.findBetween(requesterId, addresseeId)
        if (existing != null) {
            when (existing.status.code) {
                FriendshipStatus.ACCEPTED ->
                    throw AlreadyFriendsException("You are already friends with this user")
                FriendshipStatus.BLOCKED ->
                    throw UserBlockedException("Cannot send a friend request to this user")
                FriendshipStatus.PENDING -> {
                    if (existing.requester.id == requesterId)
                        throw InvalidFriendRequestException("Friend request already sent")
                    existing.status = status(FriendshipStatus.ACCEPTED)
                    existing.respondedAt = Instant.now()
                    return friendshipRepository.save(existing).toRequestResponse(requesterId)
                }
            }
        }

        val requester = userRepository.findById(requesterId)
            .orElseThrow { UserNotFoundException("User not found") }

        val friendship = friendshipRepository.save(
            Friendship(
                requester = requester,
                addressee = addressee,
                status = status(FriendshipStatus.PENDING)
            )
        )
        return friendship.toRequestResponse(requesterId)
    }

    @Transactional
    fun acceptRequest(userId: Long, friendshipId: Long): FriendResponse {
        val friendship = requireIncomingPending(userId, friendshipId)
        friendship.status = status(FriendshipStatus.ACCEPTED)
        friendship.respondedAt = Instant.now()
        return friendshipRepository.save(friendship).toFriendResponse(userId)
    }

    @Transactional
    fun declineRequest(userId: Long, friendshipId: Long) {
        val friendship = requireIncomingPending(userId, friendshipId)
        friendshipRepository.delete(friendship)
    }

    @Transactional
    fun removeFriend(userId: Long, otherUserId: Long) {
        val friendship = friendshipRepository.findBetween(userId, otherUserId)
            ?.takeIf { it.status.code == FriendshipStatus.ACCEPTED }
            ?: throw FriendshipNotFoundException("You are not friends with this user")
        friendshipRepository.delete(friendship)
    }

    @Transactional
    fun block(userId: Long, targetUserId: Long) {
        if (userId == targetUserId)
            throw InvalidFriendRequestException("Cannot block yourself")

        val target = userRepository.findById(targetUserId)
            .orElseThrow { UserNotFoundException("User not found") }

        val existing = friendshipRepository.findBetween(userId, targetUserId)
        if (existing != null) {
            if (existing.status.code == FriendshipStatus.BLOCKED && existing.requester.id == userId)
                return
            existing.requester = userRepository.findById(userId)
                .orElseThrow { UserNotFoundException("User not found") }
            existing.addressee = target
            existing.status = status(FriendshipStatus.BLOCKED)
            existing.respondedAt = Instant.now()
            friendshipRepository.save(existing)
            return
        }

        val blocker = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found") }
        friendshipRepository.save(
            Friendship(
                requester = blocker,
                addressee = target,
                status = status(FriendshipStatus.BLOCKED),
                respondedAt = Instant.now()
            )
        )
    }

    @Transactional
    fun unblock(userId: Long, targetUserId: Long) {
        val friendship = friendshipRepository.findBetween(userId, targetUserId)
            ?.takeIf { it.status.code == FriendshipStatus.BLOCKED && it.requester.id == userId }
            ?: throw FriendshipNotFoundException("This user is not blocked by you")
        friendshipRepository.delete(friendship)
    }

    @Transactional(readOnly = true)
    fun getFriends(userId: Long): List<FriendResponse> =
        friendshipRepository.findAcceptedByUserId(userId).map { it.toFriendResponse(userId) }

    @Transactional(readOnly = true)
    fun getIncomingRequests(userId: Long): List<FriendRequestResponse> =
        friendshipRepository.findIncomingByUserId(userId).map { it.toRequestResponse(userId) }

    @Transactional(readOnly = true)
    fun getOutgoingRequests(userId: Long): List<FriendRequestResponse> =
        friendshipRepository.findOutgoingByUserId(userId).map { it.toRequestResponse(userId) }

    private fun requireIncomingPending(userId: Long, friendshipId: Long): Friendship {
        val friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow { FriendshipNotFoundException("Friend request not found") }
        if (friendship.addressee.id != userId)
            throw FriendshipNotFoundException("Friend request not found")
        if (friendship.status.code != FriendshipStatus.PENDING)
            throw InvalidFriendRequestException("Friend request is no longer pending")
        return friendship
    }

    private fun Friendship.counterpart(userId: Long): User =
        if (requester.id == userId) addressee else requester

    private fun Friendship.toFriendResponse(userId: Long): FriendResponse {
        val other = counterpart(userId)
        return FriendResponse(
            userId = other.id,
            displayName = other.displayName,
            avatarUrl = other.avatarUrl,
            friendsSince = respondedAt
        )
    }

    private fun Friendship.toRequestResponse(userId: Long): FriendRequestResponse {
        val other = counterpart(userId)
        return FriendRequestResponse(
            id = id,
            userId = other.id,
            displayName = other.displayName,
            avatarUrl = other.avatarUrl,
            status = status.code,
            createdAt = createdAt
        )
    }
}
