package com.splittogether.backend.currency.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

@Component
class ExchangeRateScheduler(
    private val exchangeRateService: ExchangeRateService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${currency.refresh-cron}", zone = "Europe/Moscow")
    fun refreshDailyRates() {
        val today = LocalDate.now(ZoneId.of("Europe/Moscow"))
        try {
            exchangeRateService.refreshRates(today)
        } catch (e: Exception) {
            log.warn("Scheduled exchange rate refresh failed for {}: {}", today, e.message)
        }
    }
}
