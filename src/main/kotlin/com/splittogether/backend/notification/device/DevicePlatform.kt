package com.splittogether.backend.notification.device

import jakarta.persistence.*

@Entity
@Table(name = "device_platforms")
class DevicePlatform(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String
) {
    companion object {
        const val ANDROID = "ANDROID"
        const val IOS = "IOS"
        const val WEB = "WEB"
    }
}
