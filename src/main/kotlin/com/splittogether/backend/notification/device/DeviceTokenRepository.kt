package com.splittogether.backend.notification.device

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DeviceTokenRepository : JpaRepository<DeviceToken, Long> {
    fun findByToken(token: String): DeviceToken?
    fun findByUserId(userId: Long): List<DeviceToken>

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.token IN :tokens")
    fun deleteByTokenIn(@Param("tokens") tokens: List<String>)
}
