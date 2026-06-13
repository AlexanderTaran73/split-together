package com.splittogether.backend.balance.repository

import com.splittogether.backend.balance.entity.Balance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal

interface BalanceRepository : JpaRepository<Balance, Long> {

    interface GroupSumProjection {
        val groupId: Long
        val total: BigDecimal?
    }

    @Query("SELECT b FROM Balance b JOIN FETCH b.debtor JOIN FETCH b.creditor WHERE b.group.id = :groupId")
    fun findByGroupId(groupId: Long): List<Balance>

    @Query("SELECT b FROM Balance b JOIN FETCH b.group g JOIN FETCH g.baseCurrency WHERE b.creditor.id = :userId OR b.debtor.id = :userId")
    fun findInvolvingUser(userId: Long): List<Balance>

    fun findByGroupIdAndDebtorIdAndCreditorId(groupId: Long, debtorId: Long, creditorId: Long): Balance?

    @Query("SELECT b.group.id as groupId, SUM(b.amount) as total FROM Balance b WHERE b.creditor.id = :userId AND b.group.id IN :groupIds GROUP BY b.group.id")
    fun sumOwedInGroups(userId: Long, groupIds: List<Long>): List<GroupSumProjection>

    @Query("SELECT b.group.id as groupId, SUM(b.amount) as total FROM Balance b WHERE b.debtor.id = :userId AND b.group.id IN :groupIds GROUP BY b.group.id")
    fun sumOwingInGroups(userId: Long, groupIds: List<Long>): List<GroupSumProjection>

    @Query("SELECT SUM(b.amount) FROM Balance b WHERE b.creditor.id = :userId")
    fun sumAmountByCreditorId(userId: Long): BigDecimal?

    @Query("SELECT SUM(b.amount) FROM Balance b WHERE b.debtor.id = :userId")
    fun sumAmountByDebtorId(userId: Long): BigDecimal?

    @Query("SELECT SUM(b.amount) FROM Balance b WHERE b.creditor.id = :userId AND b.group.id = :groupId")
    fun sumOwedInGroup(userId: Long, groupId: Long): BigDecimal?

    @Query("SELECT SUM(b.amount) FROM Balance b WHERE b.debtor.id = :userId AND b.group.id = :groupId")
    fun sumOwingInGroup(userId: Long, groupId: Long): BigDecimal?

    @Modifying
    @Query("DELETE FROM Balance b WHERE b.group.id = :groupId AND b.debtor.id = :debtorId AND b.creditor.id = :creditorId")
    fun deleteByGroupIdAndDebtorIdAndCreditorId(groupId: Long, debtorId: Long, creditorId: Long)

    @Modifying
    @Query("DELETE FROM Balance b WHERE b.group.id = :groupId")
    fun deleteByGroupId(groupId: Long)
}
