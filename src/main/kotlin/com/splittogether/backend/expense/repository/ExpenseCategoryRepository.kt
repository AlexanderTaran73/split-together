package com.splittogether.backend.expense.repository

import com.splittogether.backend.expense.entity.ExpenseCategory
import org.springframework.data.jpa.repository.JpaRepository

interface ExpenseCategoryRepository : JpaRepository<ExpenseCategory, Int> {
    fun findByCode(code: String): ExpenseCategory?
}
