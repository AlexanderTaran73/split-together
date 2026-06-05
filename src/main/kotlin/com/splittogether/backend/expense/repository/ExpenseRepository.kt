package com.splittogether.backend.expense.repository

import com.splittogether.backend.expense.entity.Expense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ExpenseRepository : JpaRepository<Expense, Long> {

    interface GroupExpenseCountProjection {
        val groupId: Long
        val count: Long
    }

    @Query("SELECT e FROM Expense e JOIN FETCH e.paidBy JOIN FETCH e.currency LEFT JOIN FETCH e.category WHERE e.group.id = :groupId AND e.deletedAt IS NULL ORDER BY e.expenseDate DESC, e.createdAt DESC")
    fun findActiveByGroupId(groupId: Long): List<Expense>

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.group.id = :groupId AND e.deletedAt IS NULL")
    fun countActiveByGroupId(groupId: Long): Long

    @Query("SELECT e.group.id as groupId, COUNT(e) as count FROM Expense e WHERE e.group.id IN :groupIds AND e.deletedAt IS NULL GROUP BY e.group.id")
    fun countActiveByGroupIds(groupIds: List<Long>): List<GroupExpenseCountProjection>
}
