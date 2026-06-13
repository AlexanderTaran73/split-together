package com.splittogether.backend.notification.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "outbox_events")
class OutboxEvent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "event_type", nullable = false)
    val eventType: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    val payload: String,

    @Column(nullable = false)
    val version: Short = 1,

    @Column(nullable = false)
    var attempts: Int = 0,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "next_attempt_at", nullable = false)
    var nextAttemptAt: Instant = Instant.now(),

    @Column(name = "processed_at")
    var processedAt: Instant? = null,

    @Column(name = "failed_at")
    var failedAt: Instant? = null
)
