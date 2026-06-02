package com.splittogether.backend.group.entity

import jakarta.persistence.*

@Entity
@Table(name = "invitation_statuses")
class InvitationStatus(
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
        const val DECLINED = "DECLINED"
        const val REVOKED = "REVOKED"
        const val EXPIRED = "EXPIRED"
    }
}
