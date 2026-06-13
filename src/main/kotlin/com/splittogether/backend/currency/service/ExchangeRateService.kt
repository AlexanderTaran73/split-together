package com.splittogether.backend.currency.service

import com.splittogether.backend.common.entity.Currency
import com.splittogether.backend.common.exception.ExchangeRateUnavailableException
import com.splittogether.backend.common.repository.CurrencyRepository
import com.splittogether.backend.currency.entity.ExchangeRate
import com.splittogether.backend.currency.provider.ExchangeRateProvider
import com.splittogether.backend.currency.repository.ExchangeRateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class ExchangeRateService(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val currencyRepository: CurrencyRepository,
    private val exchangeRateProvider: ExchangeRateProvider
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun convert(amount: BigDecimal, from: Currency, to: Currency, date: LocalDate): BigDecimal {
        if (from.id == to.id) return amount.setScale(2, RoundingMode.HALF_UP)
        return amount.multiply(getRateToRub(from, date))
            .divide(getRateToRub(to, date), 2, RoundingMode.HALF_UP)
    }

    @Transactional
    fun getRateToRub(currency: Currency, date: LocalDate): BigDecimal {
        if (currency.code == RUB) return BigDecimal.ONE

        exchangeRateRepository.findByCurrencyIdAndRateDate(currency.id, date)?.let { return it.rate }

        if (!exchangeRateRepository.existsByRateDate(date)) {
            try {
                refreshRates(date)
            } catch (e: Exception) {
                log.warn("Failed to fetch exchange rates for {}: {}", date, e.message)
            }
            exchangeRateRepository.findByCurrencyIdAndRateDate(currency.id, date)?.let { return it.rate }
        }

        return exchangeRateRepository
            .findTopByCurrencyIdAndRateDateLessThanEqualOrderByRateDateDesc(currency.id, date)
            ?.rate
            ?: throw ExchangeRateUnavailableException("No exchange rate available for ${currency.code} on $date")
    }

    @Transactional(readOnly = true)
    fun convertForDisplay(amount: BigDecimal, from: Currency, to: Currency, date: LocalDate): BigDecimal {
        if (from.id == to.id) return amount.setScale(2, RoundingMode.HALF_UP)
        return amount.multiply(latestRateToRub(from, date))
            .divide(latestRateToRub(to, date), 2, RoundingMode.HALF_UP)
    }

    private fun latestRateToRub(currency: Currency, date: LocalDate): BigDecimal {
        if (currency.code == RUB) return BigDecimal.ONE
        return (exchangeRateRepository.findByCurrencyIdAndRateDate(currency.id, date)
            ?: exchangeRateRepository.findTopByCurrencyIdAndRateDateLessThanEqualOrderByRateDateDesc(currency.id, date))
            ?.rate
            ?: throw ExchangeRateUnavailableException("No exchange rate available for ${currency.code} on $date")
    }

    @Transactional
    fun refreshRates(date: LocalDate) {
        val rates = exchangeRateProvider.fetchRates(date)
        val currenciesByCode = currencyRepository.findAll().associateBy { it.code }

        rates.forEach { (code, rate) ->
            val currency = currenciesByCode[code] ?: return@forEach
            val existing = exchangeRateRepository.findByCurrencyIdAndRateDate(currency.id, date)
            if (existing != null) {
                existing.rate = rate
                exchangeRateRepository.save(existing)
            } else {
                exchangeRateRepository.save(ExchangeRate(currency = currency, rateDate = date, rate = rate))
            }
        }
        log.info("Stored {} exchange rates for {}", rates.size, date)
    }

    companion object {
        const val RUB = "RUB"
    }
}
