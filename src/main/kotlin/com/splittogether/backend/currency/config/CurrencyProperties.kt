package com.splittogether.backend.currency.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "currency")
data class CurrencyProperties(
    val cbrUrl: String = "https://www.cbr.ru/scripts/XML_daily.asp",
    val refreshCron: String = "0 0 16 * * *"
)
