package com.splittogether.backend.auth.entity

import com.splittogether.backend.common.entity.EmailVerificationPurpose
import com.splittogether.backend.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Entity
@Table(name = "email_verifications")
class EmailVerification(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    val code: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purpose_id", nullable = false)
    val purpose: EmailVerificationPurpose,

    @Column(name = "new_email")
    val newEmail: String? = null,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "used_at")
    var usedAt: Instant? = null
)
