package com.splittogether.backend.settlement.repository

import com.splittogether.backend.settlement.entity.Settlement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SettlementRepository : JpaRepository<Settlement, Long> {
    fun findByGroupIdOrderByCreatedAtDesc(groupId: Long): List<Settlement>

    @Query("SELECT s FROM Settlement s JOIN FETCH s.payer JOIN FETCH s.receiver JOIN FETCH s.currency WHERE s.group.id = :groupId AND s.status.code = :statusCode")
    fun findByGroupIdAndStatusCode(groupId: Long, statusCode: String): List<Settlement>
}
