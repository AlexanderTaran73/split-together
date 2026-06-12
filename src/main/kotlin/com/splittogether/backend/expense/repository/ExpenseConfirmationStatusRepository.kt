package com.splittogether.backend.expense.repository

import com.splittogether.backend.expense.entity.ExpenseConfirmationStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ExpenseConfirmationStatusRepository : JpaRepository<ExpenseConfirmationStatus, Int> {
    fun findByCode(code: String): ExpenseConfirmationStatus?
}
