package com.splittogether.backend.notification.preference

import jakarta.persistence.*

@Entity
@Table(name = "notification_channels")
class NotificationChannel(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String
) {
    companion object {
        const val EMAIL = "EMAIL"
        const val PUSH = "PUSH"
    }
}
