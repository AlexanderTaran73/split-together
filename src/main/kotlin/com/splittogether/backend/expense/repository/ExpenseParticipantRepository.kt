package com.splittogether.backend.expense.repository

import com.splittogether.backend.expense.entity.ExpenseParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ExpenseParticipantRepository : JpaRepository<ExpenseParticipant, Long> {
    fun findByExpenseId(expenseId: Long): List<ExpenseParticipant>

    @Modifying
    @Query("DELETE FROM ExpenseParticipant ep WHERE ep.expense.id = :expenseId")
    fun deleteByExpenseId(expenseId: Long)
}
