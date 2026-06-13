package com.splittogether.backend.currency.provider

import com.splittogether.backend.currency.config.CurrencyProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.w3c.dom.Element
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

@Component
class CbrExchangeRateProvider(
    private val properties: CurrencyProperties
) : ExchangeRateProvider {

    private val restClient = RestClient.create()

    override fun fetchRates(date: LocalDate): Map<String, BigDecimal> {
        val xml = restClient.get()
            .uri("${properties.cbrUrl}?date_req=${date.format(CBR_DATE_FORMAT)}")
            .retrieve()
            .body(ByteArray::class.java)
            ?: throw IllegalStateException("Empty response from CBR for date $date")
        return parseRates(xml)
    }

    companion object {
        private val CBR_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        internal fun parseRates(xml: ByteArray): Map<String, BigDecimal> {
            val factory = DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
            val document = factory.newDocumentBuilder().parse(xml.inputStream())
            val valutes = document.getElementsByTagName("Valute")

            val rates = mutableMapOf<String, BigDecimal>()
            for (i in 0 until valutes.length) {
                val valute = valutes.item(i) as Element
                val code = valute.childText("CharCode") ?: continue
                val nominal = valute.childText("Nominal")?.toBigDecimalOrNull() ?: continue
                val value = valute.childText("Value")?.replace(',', '.')?.toBigDecimalOrNull() ?: continue
                if (nominal.compareTo(BigDecimal.ZERO) == 0) continue
                rates[code] = value.divide(nominal, 6, RoundingMode.HALF_UP)
            }
            return rates
        }

        private fun Element.childText(tag: String): String? =
            getElementsByTagName(tag).item(0)?.textContent?.trim()?.takeIf { it.isNotEmpty() }
    }
}
