package com.splittogether.backend.friendship.repository

import com.splittogether.backend.friendship.entity.FriendshipStatus
import org.springframework.data.jpa.repository.JpaRepository

interface FriendshipStatusRepository : JpaRepository<FriendshipStatus, Int> {
    fun findByCode(code: String): FriendshipStatus?
}
