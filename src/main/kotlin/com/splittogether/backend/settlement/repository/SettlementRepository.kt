package com.splittogether.backend.settlement.repository

import com.splittogether.backend.settlement.entity.Settlement
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementRepository : JpaRepository<Settlement, Long> {
    fun findByGroupIdOrderByCreatedAtDesc(groupId: Long): List<Settlement>
}
