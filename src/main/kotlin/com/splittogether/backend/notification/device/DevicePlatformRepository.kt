package com.splittogether.backend.notification.device

import org.springframework.data.jpa.repository.JpaRepository

interface DevicePlatformRepository : JpaRepository<DevicePlatform, Int> {
    fun findByCode(code: String): DevicePlatform?
}
