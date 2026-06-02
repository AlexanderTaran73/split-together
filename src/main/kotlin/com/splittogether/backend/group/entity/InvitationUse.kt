package com.splittogether.backend.group.entity

import com.splittogether.backend.user.entity.User
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "invitation_uses")
class InvitationUse(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id", nullable = false)
    val invitation: GroupInvitation,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "used_at", nullable = false)
    val usedAt: Instant = Instant.now()
)
