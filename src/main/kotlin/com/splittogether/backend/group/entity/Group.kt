package com.splittogether.backend.group.entity

import com.splittogether.backend.common.entity.Currency
import com.splittogether.backend.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(name = "groups")
class Group(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @Column
    var description: String? = null,

    @Column(name = "avatar_object_key")
    var avatarObjectKey: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_currency_id", nullable = false)
    var baseCurrency: Currency,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", nullable = false)
    var status: GroupStatus,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "archived_at")
    var archivedAt: Instant? = null
)
