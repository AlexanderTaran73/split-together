package com.splittogether.backend.file.entity

import com.splittogether.backend.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(name = "files")
class StoredFile(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_type_id", nullable = false)
    val ownerType: FileOwnerType,

    @Column(name = "owner_id", nullable = false)
    val ownerId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    val uploadedBy: User,

    @Column(name = "object_key", nullable = false, unique = true)
    val objectKey: String,

    @Column(name = "original_name", nullable = false)
    val originalName: String,

    @Column(name = "content_type")
    val contentType: String? = null,

    @Column(name = "size_bytes", nullable = false)
    val sizeBytes: Long,

    @Column
    var description: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    var deletedBy: User? = null
)
