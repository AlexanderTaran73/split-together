package com.splittogether.backend.common.repository

import com.splittogether.backend.common.entity.Currency
import org.springframework.data.jpa.repository.JpaRepository

interface CurrencyRepository : JpaRepository<Currency, Int> {
    fun findByCode(code: String): Currency?
}
