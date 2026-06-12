package com.splittogether.backend.user.entity

import jakarta.persistence.*

@Entity
@Table(name = "group_invite_policies")
class GroupInvitePolicy(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String
) {
    companion object {
        const val ANYONE = "ANYONE"
        const val FRIENDS = "FRIENDS"
        const val INVITE_ONLY = "INVITE_ONLY"
    }
}
