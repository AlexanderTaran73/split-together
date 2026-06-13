package com.splittogether.backend.currency.service

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.common.entity.Currency
import com.splittogether.backend.common.exception.ExchangeRateUnavailableException
import com.splittogether.backend.common.repository.CurrencyRepository
import com.splittogether.backend.currency.repository.ExchangeRateRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

class ExchangeRateServiceTest : AbstractIntegrationTest() {

    @Autowired private lateinit var exchangeRateService: ExchangeRateService
    @Autowired private lateinit var exchangeRateRepository: ExchangeRateRepository
    @Autowired private lateinit var currencyRepository: CurrencyRepository

    private val date: LocalDate = LocalDate.of(2026, 6, 1)

    private fun currency(code: String): Currency =
        currencyRepository.findByCode(code) ?: error("Missing currency $code")

    @Test
    fun `getRateToRub returns ONE for RUB without touching provider`() {
        assertEquals(BigDecimal.ONE, exchangeRateService.getRateToRub(currency("RUB"), date))
        assertEquals(0, stubExchangeRateProvider.fetchCount)
    }

    @Test
    fun `convert uses rate for exact date`() {
        stubExchangeRateProvider.setRates(date, mapOf("USD" to BigDecimal("80")))

        val result = exchangeRateService.convert(BigDecimal("10.00"), currency("USD"), currency("RUB"), date)

        assertEquals(BigDecimal("800.00"), result)
    }

    @Test
    fun `convert computes cross rate through RUB with HALF_UP rounding`() {
        stubExchangeRateProvider.setRates(date, mapOf("USD" to BigDecimal("80"), "EUR" to BigDecimal("90")))

        val result = exchangeRateService.convert(BigDecimal("9.00"), currency("EUR"), currency("USD"), date)

        assertEquals(BigDecimal("10.13"), result)
    }

    @Test
    fun `convert returns amount unchanged for same currency`() {
        val result = exchangeRateService.convert(BigDecimal("12.34"), currency("USD"), currency("USD"), date)

        assertEquals(BigDecimal("12.34"), result)
        assertEquals(0, stubExchangeRateProvider.fetchCount)
    }

    @Test
    fun `lazy fetch stores rates once and reuses them`() {
        stubExchangeRateProvider.setRates(date, mapOf("USD" to BigDecimal("80")))

        exchangeRateService.convert(BigDecimal("10.00"), currency("USD"), currency("RUB"), date)
        exchangeRateService.convert(BigDecimal("20.00"), currency("USD"), currency("RUB"), date)

        assertEquals(1, stubExchangeRateProvider.fetchCount)
        assertEquals(1, exchangeRateRepository.count())
    }

    @Test
    fun `falls back to latest available rate before the date`() {
        val earlier = date.minusDays(5)
        stubExchangeRateProvider.setRates(earlier, mapOf("USD" to BigDecimal("75")))
        exchangeRateService.refreshRates(earlier)

        val result = exchangeRateService.convert(BigDecimal("10.00"), currency("USD"), currency("RUB"), date)

        assertEquals(BigDecimal("750.00"), result)
    }

    @Test
    fun `falls back to latest available rate when provider is down`() {
        val earlier = date.minusDays(3)
        stubExchangeRateProvider.setRates(earlier, mapOf("USD" to BigDecimal("75")))
        exchangeRateService.refreshRates(earlier)
        stubExchangeRateProvider.failing = true

        val result = exchangeRateService.convert(BigDecimal("10.00"), currency("USD"), currency("RUB"), date)

        assertEquals(BigDecimal("750.00"), result)
    }

    @Test
    fun `throws when no rate is available at all`() {
        assertThrows<ExchangeRateUnavailableException> {
            exchangeRateService.getRateToRub(currency("USD"), date)
        }
    }

    @Test
    fun `refreshRates upserts existing rows and skips unknown currencies`() {
        stubExchangeRateProvider.setRates(date, mapOf("USD" to BigDecimal("80"), "ZZZ" to BigDecimal("1")))
        exchangeRateService.refreshRates(date)

        stubExchangeRateProvider.setRates(date, mapOf("USD" to BigDecimal("81")))
        exchangeRateService.refreshRates(date)

        assertEquals(1, exchangeRateRepository.count())
        assertEquals(0, BigDecimal("81").compareTo(exchangeRateService.getRateToRub(currency("USD"), date)))
    }
}
