package com.splittogether.backend.notification.preference

import org.springframework.data.jpa.repository.JpaRepository

interface NotificationChannelRepository : JpaRepository<NotificationChannel, Int> {
    fun findByCode(code: String): NotificationChannel?
}
