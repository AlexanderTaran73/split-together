package com.splittogether.backend.user.repository

import com.splittogether.backend.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean

    @Query(
        "SELECT u FROM User u WHERE u.id <> :requesterId AND (" +
        "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
        "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%')))"
    )
    fun search(requesterId: Long, query: String): List<User>
}
