package com.splittogether.backend.settlement.repository

import com.splittogether.backend.settlement.entity.SettlementStatus
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementStatusRepository : JpaRepository<SettlementStatus, Int> {
    fun findByCode(code: String): SettlementStatus?
}
