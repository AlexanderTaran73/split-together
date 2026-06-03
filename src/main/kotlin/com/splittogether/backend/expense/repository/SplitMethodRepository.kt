package com.splittogether.backend.expense.repository

import com.splittogether.backend.expense.entity.SplitMethod
import org.springframework.data.jpa.repository.JpaRepository

interface SplitMethodRepository : JpaRepository<SplitMethod, Int> {
    fun findByCode(code: String): SplitMethod?
}
