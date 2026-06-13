package com.splittogether.backend.currency.provider

import java.math.BigDecimal
import java.time.LocalDate

interface ExchangeRateProvider {
    fun fetchRates(date: LocalDate): Map<String, BigDecimal>
}
