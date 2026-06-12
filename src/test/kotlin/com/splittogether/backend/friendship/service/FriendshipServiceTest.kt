package com.splittogether.backend.friendship.service

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.common.exception.AlreadyFriendsException
import com.splittogether.backend.common.exception.FriendshipNotFoundException
import com.splittogether.backend.common.exception.InvalidFriendRequestException
import com.splittogether.backend.common.exception.UserBlockedException
import com.splittogether.backend.common.exception.UserNotFoundException
import com.splittogether.backend.friendship.entity.FriendshipStatus
import com.splittogether.backend.friendship.repository.FriendshipRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FriendshipServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var friendshipService: FriendshipService
    @Autowired private lateinit var friendshipRepository: FriendshipRepository

    // ── sendRequest ─────────────────────────────────────────────────────────────

    @Test
    fun `sendRequest creates a PENDING friendship`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")

        val response = friendshipService.sendRequest(a.id, b.id)

        assertEquals(b.id, response.userId)
        assertEquals(FriendshipStatus.PENDING, response.status)
        val saved = friendshipRepository.findBetween(a.id, b.id)
        assertNotNull(saved)
        assertEquals(a.id, saved!!.requester.id)
        assertEquals(b.id, saved.addressee.id)
        assertEquals(FriendshipStatus.PENDING, saved.status.code)
    }

    @Test
    fun `sendRequest to self throws`() {
        val a = generator.user()
        assertFailsWith<InvalidFriendRequestException> {
            friendshipService.sendRequest(a.id, a.id)
        }
    }

    @Test
    fun `sendRequest to unknown user throws`() {
        val a = generator.user()
        assertFailsWith<UserNotFoundException> {
            friendshipService.sendRequest(a.id, 999L)
        }
    }

    @Test
    fun `sendRequest twice in same direction throws`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        friendshipService.sendRequest(a.id, b.id)

        assertFailsWith<InvalidFriendRequestException> {
            friendshipService.sendRequest(a.id, b.id)
        }
    }

    @Test
    fun `sendRequest when already friends throws`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        val req = friendshipService.sendRequest(a.id, b.id)
        friendshipService.acceptRequest(b.id, req.id)

        assertFailsWith<AlreadyFriendsException> {
            friendshipService.sendRequest(a.id, b.id)
        }
    }

    @Test
    fun `sendRequest accepts a reciprocal pending request`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        friendshipService.sendRequest(a.id, b.id)

        val response = friendshipService.sendRequest(b.id, a.id)

        assertEquals(FriendshipStatus.ACCEPTED, response.status)
        assertTrue(friendshipService.areFriends(a.id, b.id))
        // одна строка на пару, направление не задвоилось
        assertEquals(1, friendshipRepository.findAll().size)
    }

    @Test
    fun `sendRequest to a user who blocked you throws`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        friendshipService.block(b.id, a.id)

        assertFailsWith<UserBlockedException> {
            friendshipService.sendRequest(a.id, b.id)
        }
    }

    // ── acceptRequest ───────────────────────────────────────────────────────────

    @Test
    fun `acceptRequest makes both users friends`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        val req = friendshipService.sendRequest(a.id, b.id)

        val response = friendshipService.acceptRequest(b.id, req.id)

        assertEquals(a.id, response.userId)
        assertNotNull(response.friendsSince)
        assertTrue(friendshipService.areFriends(a.id, b.id))
    }

    @Test
    fun `acceptRequest by the requester throws not found`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        val req = friendshipService.sendRequest(a.id, b.id)

        assertFailsWith<FriendshipNotFoundException> {
            friendshipService.acceptRequest(a.id, req.id)
        }
    }

    @Test
    fun `acceptRequest on unknown id throws not found`() {
        val a = generator.user()
        assertFailsWith<FriendshipNotFoundException> {
            friendshipService.acceptRequest(a.id, 999L)
        }
    }

    // ── declineRequest ──────────────────────────────────────────────────────────

    @Test
    fun `declineRequest removes the request and allows re-sending`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        val req = friendshipService.sendRequest(a.id, b.id)

        friendshipService.declineRequest(b.id, req.id)

        assertNull(friendshipRepository.findBetween(a.id, b.id))
        // повторная заявка снова проходит
        friendshipService.sendRequest(a.id, b.id)
        assertNotNull(friendshipRepository.findBetween(a.id, b.id))
    }

    @Test
    fun `declineRequest by requester throws not found`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        val req = friendshipService.sendRequest(a.id, b.id)

        assertFailsWith<FriendshipNotFoundException> {
            friendshipService.declineRequest(a.id, req.id)
        }
    }

    // ── removeFriend ────────────────────────────────────────────────────────────

    @Test
    fun `removeFriend deletes the friendship`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        val req = friendshipService.sendRequest(a.id, b.id)
        friendshipService.acceptRequest(b.id, req.id)

        friendshipService.removeFriend(a.id, b.id)

        assertNull(friendshipRepository.findBetween(a.id, b.id))
        assertTrue(!friendshipService.areFriends(a.id, b.id))
    }

    @Test
    fun `removeFriend when not friends throws not found`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        friendshipService.sendRequest(a.id, b.id)

        assertFailsWith<FriendshipNotFoundException> {
            friendshipService.removeFriend(a.id, b.id)
        }
    }

    // ── block / unblock ─────────────────────────────────────────────────────────

    @Test
    fun `block creates a BLOCKED relationship with blocker as requester`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")

        friendshipService.block(a.id, b.id)

        val rel = friendshipRepository.findBetween(a.id, b.id)
        assertNotNull(rel)
        assertEquals(FriendshipStatus.BLOCKED, rel!!.status.code)
        assertEquals(a.id, rel.requester.id)
        assertEquals(b.id, rel.addressee.id)
        assertTrue(friendshipService.isBlockedBetween(a.id, b.id))
    }

    @Test
    fun `block overrides an existing friendship`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        val req = friendshipService.sendRequest(a.id, b.id)
        friendshipService.acceptRequest(b.id, req.id)

        friendshipService.block(a.id, b.id)

        assertTrue(!friendshipService.areFriends(a.id, b.id))
        assertTrue(friendshipService.isBlockedBetween(a.id, b.id))
        assertEquals(1, friendshipRepository.findAll().size)
    }

    @Test
    fun `block twice is idempotent`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        friendshipService.block(a.id, b.id)
        friendshipService.block(a.id, b.id)

        assertEquals(1, friendshipRepository.findAll().size)
    }

    @Test
    fun `block self throws`() {
        val a = generator.user()
        assertFailsWith<InvalidFriendRequestException> {
            friendshipService.block(a.id, a.id)
        }
    }

    @Test
    fun `unblock removes the block`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        friendshipService.block(a.id, b.id)

        friendshipService.unblock(a.id, b.id)

        assertNull(friendshipRepository.findBetween(a.id, b.id))
    }

    @Test
    fun `unblock by the blocked user throws not found`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")
        friendshipService.block(a.id, b.id)

        assertFailsWith<FriendshipNotFoundException> {
            friendshipService.unblock(b.id, a.id)
        }
    }

    // ── listings ────────────────────────────────────────────────────────────────

    @Test
    fun `getFriends returns accepted friends only`() {
        val me = generator.user(email = "me@test.com")
        val friend = generator.user(email = "friend@test.com")
        val pending = generator.user(email = "pending@test.com")
        val req = friendshipService.sendRequest(me.id, friend.id)
        friendshipService.acceptRequest(friend.id, req.id)
        friendshipService.sendRequest(me.id, pending.id)

        val friends = friendshipService.getFriends(me.id)

        assertEquals(1, friends.size)
        assertEquals(friend.id, friends[0].userId)
    }

    @Test
    fun `getIncoming and getOutgoing reflect request direction`() {
        val me = generator.user(email = "me@test.com")
        val sender = generator.user(email = "sender@test.com")
        val target = generator.user(email = "target@test.com")
        friendshipService.sendRequest(sender.id, me.id)
        friendshipService.sendRequest(me.id, target.id)

        val incoming = friendshipService.getIncomingRequests(me.id)
        val outgoing = friendshipService.getOutgoingRequests(me.id)

        assertEquals(1, incoming.size)
        assertEquals(sender.id, incoming[0].userId)
        assertEquals(1, outgoing.size)
        assertEquals(target.id, outgoing[0].userId)
    }
}
