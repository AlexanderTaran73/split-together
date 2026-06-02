package com.splittogether.backend.group.entity

import jakarta.persistence.*

@Entity
@Table(name = "invitation_types")
class InvitationType(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String
) {
    companion object {
        const val LINK = "LINK"
        const val DIRECT = "DIRECT"
    }
}
