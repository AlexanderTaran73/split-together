package com.splittogether.backend.currency

import com.splittogether.backend.currency.provider.ExchangeRateProvider
import java.math.BigDecimal
import java.time.LocalDate

class StubExchangeRateProvider : ExchangeRateProvider {

    private val ratesByDate = mutableMapOf<LocalDate, Map<String, BigDecimal>>()
    var failing = false
    var fetchCount = 0
        private set

    fun setRates(date: LocalDate, rates: Map<String, BigDecimal>) {
        ratesByDate[date] = rates
    }

    fun reset() {
        ratesByDate.clear()
        failing = false
        fetchCount = 0
    }

    override fun fetchRates(date: LocalDate): Map<String, BigDecimal> {
        fetchCount++
        if (failing) throw IllegalStateException("Exchange rate provider unavailable (stub)")
        return ratesByDate[date] ?: emptyMap()
    }
}
