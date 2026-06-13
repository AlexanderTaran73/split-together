package com.splittogether.backend.notification.preference

import org.springframework.data.jpa.repository.JpaRepository

interface NotificationTypeRepository : JpaRepository<NotificationType, Int> {
    fun findByCode(code: String): NotificationType?
}
