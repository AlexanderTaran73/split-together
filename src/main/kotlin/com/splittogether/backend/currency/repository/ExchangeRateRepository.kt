package com.splittogether.backend.currency.repository

import com.splittogether.backend.currency.entity.ExchangeRate
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ExchangeRateRepository : JpaRepository<ExchangeRate, Long> {
    fun findByCurrencyIdAndRateDate(currencyId: Int, rateDate: LocalDate): ExchangeRate?
    fun findTopByCurrencyIdAndRateDateLessThanEqualOrderByRateDateDesc(currencyId: Int, rateDate: LocalDate): ExchangeRate?
    fun existsByCurrencyIdAndRateDate(currencyId: Int, rateDate: LocalDate): Boolean
    fun existsByRateDate(rateDate: LocalDate): Boolean
}
