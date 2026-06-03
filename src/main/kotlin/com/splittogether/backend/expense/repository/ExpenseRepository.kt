package com.splittogether.backend.expense.repository

import com.splittogether.backend.expense.entity.Expense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ExpenseRepository : JpaRepository<Expense, Long> {

    @Query("SELECT e FROM Expense e WHERE e.group.id = :groupId AND e.deletedAt IS NULL ORDER BY e.expenseDate DESC, e.createdAt DESC")
    fun findActiveByGroupId(groupId: Long): List<Expense>
}
