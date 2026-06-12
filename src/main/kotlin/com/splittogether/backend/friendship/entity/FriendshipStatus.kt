package com.splittogether.backend.friendship.entity

import jakarta.persistence.*

@Entity
@Table(name = "friendship_statuses")
class FriendshipStatus(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String
) {
    companion object {
        const val PENDING = "PENDING"
        const val ACCEPTED = "ACCEPTED"
        const val BLOCKED = "BLOCKED"
    }
}
