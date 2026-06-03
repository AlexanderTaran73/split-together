package com.splittogether.backend.balance.repository

import com.splittogether.backend.balance.entity.Balance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface BalanceRepository : JpaRepository<Balance, Long> {
    fun findByGroupId(groupId: Long): List<Balance>
    fun findByGroupIdAndDebtorIdAndCreditorId(groupId: Long, debtorId: Long, creditorId: Long): Balance?

    @Modifying
    @Query("DELETE FROM Balance b WHERE b.group.id = :groupId AND b.debtor.id = :debtorId AND b.creditor.id = :creditorId")
    fun deleteByGroupIdAndDebtorIdAndCreditorId(groupId: Long, debtorId: Long, creditorId: Long)

    @Modifying
    @Query("DELETE FROM Balance b WHERE b.group.id = :groupId")
    fun deleteByGroupId(groupId: Long)
}
