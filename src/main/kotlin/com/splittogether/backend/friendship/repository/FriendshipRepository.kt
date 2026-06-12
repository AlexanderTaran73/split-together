package com.splittogether.backend.friendship.repository

import com.splittogether.backend.friendship.entity.Friendship
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FriendshipRepository : JpaRepository<Friendship, Long> {

    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester.id = :a AND f.addressee.id = :b)
           OR (f.requester.id = :b AND f.addressee.id = :a)
    """)
    fun findBetween(a: Long, b: Long): Friendship?

    @Query("""
        SELECT f FROM Friendship f
        WHERE f.status.code = 'ACCEPTED'
          AND (f.requester.id = :userId OR f.addressee.id = :userId)
        ORDER BY f.respondedAt DESC
    """)
    fun findAcceptedByUserId(userId: Long): List<Friendship>

    @Query("""
        SELECT f FROM Friendship f
        WHERE f.status.code = 'PENDING' AND f.addressee.id = :userId
        ORDER BY f.createdAt DESC
    """)
    fun findIncomingByUserId(userId: Long): List<Friendship>

    @Query("""
        SELECT f FROM Friendship f
        WHERE f.status.code = 'PENDING' AND f.requester.id = :userId
        ORDER BY f.createdAt DESC
    """)
    fun findOutgoingByUserId(userId: Long): List<Friendship>

    @Query("""
        SELECT COUNT(f) > 0 FROM Friendship f
        WHERE f.status.code = 'ACCEPTED'
          AND ((f.requester.id = :a AND f.addressee.id = :b)
            OR (f.requester.id = :b AND f.addressee.id = :a))
    """)
    fun areFriends(a: Long, b: Long): Boolean

    @Query("""
        SELECT COUNT(f) > 0 FROM Friendship f
        WHERE f.status.code = 'BLOCKED'
          AND ((f.requester.id = :a AND f.addressee.id = :b)
            OR (f.requester.id = :b AND f.addressee.id = :a))
    """)
    fun isBlockedBetween(a: Long, b: Long): Boolean
}
