package com.splittogether.backend.user.entity

import com.splittogether.backend.common.entity.PlatformRole
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

    @Column(name = "avatar_url")
    var avatarUrl: String? = null,

    @Column(name = "email_verified_at")
    var emailVerifiedAt: Instant? = null,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_platform_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    val platformRoles: MutableSet<PlatformRole> = mutableSetOf(),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
