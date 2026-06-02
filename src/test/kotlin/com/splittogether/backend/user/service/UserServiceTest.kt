package com.splittogether.backend.user.service

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.common.exception.UserNotFoundException
import com.splittogether.backend.user.dto.UpdateProfileRequest
import com.splittogether.backend.user.entity.User
import com.splittogether.backend.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UserServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var userService: UserService
    @Autowired private lateinit var userRepository: UserRepository

    private fun createUser(
        email: String = "user@test.com",
        displayName: String = "Test User"
    ): User = userRepository.save(User(email = email, passwordHash = "hash", displayName = displayName))

    // ── getMe ─────────────────────────────────────────────────────────────────

    @Test
    fun `getMe returns correct user response`() {
        val user = createUser()

        val response = userService.getMe(user.id)

        assertEquals(user.id, response.id)
        assertEquals(user.email, response.email)
        assertEquals(user.displayName, response.displayName)
    }

    @Test
    fun `getMe throws UserNotFoundException for unknown id`() {
        assertFailsWith<UserNotFoundException> {
            userService.getMe(999L)
        }
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Test
    fun `search returns users matching displayName`() {
        createUser(displayName = "Alice Smith")
        createUser(email = "other@test.com", displayName = "Bob Jones")

        val results = userService.search("Alice")

        assertEquals(1, results.size)
        assertEquals("Alice Smith", results[0].displayName)
    }

    @Test
    fun `search returns users matching email`() {
        createUser(email = "alice@test.com")
        createUser(email = "bob@test.com")

        val results = userService.search("alice")

        assertEquals(1, results.size)
        assertEquals("alice@test.com", results[0].email)
    }

    @Test
    fun `search is case insensitive`() {
        createUser(displayName = "Alice Smith")

        val results = userService.search("ALICE")

        assertEquals(1, results.size)
    }

    @Test
    fun `search returns empty list when no match`() {
        createUser()

        val results = userService.search("xyz_no_match")

        assertTrue(results.isEmpty())
    }

    // ── updateMe ──────────────────────────────────────────────────────────────

    @Test
    fun `updateMe updates displayName and persists the change`() {
        val user = createUser(displayName = "Old Name")

        val response = userService.updateMe(user.id, UpdateProfileRequest("New Name"))

        assertEquals("New Name", response.displayName)
        assertEquals("New Name", userRepository.findById(user.id).get().displayName)
    }

    @Test
    fun `updateMe throws UserNotFoundException for unknown id`() {
        assertFailsWith<UserNotFoundException> {
            userService.updateMe(999L, UpdateProfileRequest("Name"))
        }
    }
}
