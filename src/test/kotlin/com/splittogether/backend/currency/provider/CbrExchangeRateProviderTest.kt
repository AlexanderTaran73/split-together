package com.splittogether.backend.currency.provider

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CbrExchangeRateProviderTest {

    private fun sampleXml(): ByteArray = """
        <?xml version="1.0" encoding="windows-1251"?>
        <ValCurs Date="13.06.2026" name="Foreign Currency Market">
            <Valute ID="R01235">
                <NumCode>840</NumCode>
                <CharCode>USD</CharCode>
                <Nominal>1</Nominal>
                <Name>Доллар США</Name>
                <Value>79,5000</Value>
            </Valute>
            <Valute ID="R01239">
                <NumCode>978</NumCode>
                <CharCode>EUR</CharCode>
                <Nominal>1</Nominal>
                <Name>Евро</Name>
                <Value>90,1234</Value>
            </Valute>
            <Valute ID="R01335">
                <NumCode>398</NumCode>
                <CharCode>KZT</CharCode>
                <Nominal>100</Nominal>
                <Name>Казахстанских тенге</Name>
                <Value>15,2591</Value>
            </Valute>
            <Valute ID="R01820">
                <NumCode>392</NumCode>
                <CharCode>JPY</CharCode>
                <Nominal>100</Nominal>
                <Name>Японских иен</Name>
                <Value>54,1374</Value>
            </Valute>
        </ValCurs>
    """.trimIndent().toByteArray(charset("windows-1251"))

    @Test
    fun `parses rates normalizing nominal and comma decimal separator`() {
        val rates = CbrExchangeRateProvider.parseRates(sampleXml())

        assertEquals(4, rates.size)
        assertEquals(BigDecimal("79.500000"), rates["USD"])
        assertEquals(BigDecimal("90.123400"), rates["EUR"])
        assertEquals(BigDecimal("0.152591"), rates["KZT"])
        assertEquals(BigDecimal("0.541374"), rates["JPY"])
    }

    @Test
    fun `skips malformed valute entries`() {
        val xml = """
            <?xml version="1.0" encoding="windows-1251"?>
            <ValCurs Date="13.06.2026" name="Foreign Currency Market">
                <Valute ID="R01235">
                    <NumCode>840</NumCode>
                    <CharCode>USD</CharCode>
                    <Nominal>1</Nominal>
                    <Name>Доллар США</Name>
                    <Value>79,5000</Value>
                </Valute>
                <Valute ID="R00000">
                    <NumCode>000</NumCode>
                    <CharCode>BAD</CharCode>
                    <Nominal>1</Nominal>
                    <Name>Сломанная</Name>
                    <Value>not-a-number</Value>
                </Valute>
            </ValCurs>
        """.trimIndent().toByteArray(charset("windows-1251"))

        val rates = CbrExchangeRateProvider.parseRates(xml)

        assertEquals(BigDecimal("79.500000"), rates["USD"])
        assertNull(rates["BAD"])
    }
}
