package com.example.mybudgettree

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {

    @Test
    fun format_inTheRand_addsRandPrefixAndTwoDecimals() {
        assertEquals("R1,234.50", MoneyFormatter.format("ZAR", 1234.5))
    }

    @Test
    fun format_zero() {
        assertEquals("R0.00", MoneyFormatter.format("ZAR", 0.0))
    }

    @Test
    fun formatSigned_income_hasNoMinus() {
        assertEquals("R100.00", MoneyFormatter.formatSigned("ZAR", 100.0, isIncome = true))
    }

    @Test
    fun formatSigned_expense_hasMinus() {
        assertEquals("-R100.00", MoneyFormatter.formatSigned("ZAR", 100.0, isIncome = false))
    }

    @Test
    fun formatSigned_expense_negativeInputAmount_stillShowsSingleMinus() {
        // amount is stored positive in practice, but the formatter should be robust to a
        // negative input rather than printing a double minus sign
        assertEquals("-R100.00", MoneyFormatter.formatSigned("ZAR", -100.0, isIncome = false))
    }

    @Test
    fun compact_belowThousand_showsPlainInteger() {
        assertEquals("999", MoneyFormatter.compact(999.0))
    }

    @Test
    fun compact_thousands_showsKSuffix() {
        assertEquals("1.5k", MoneyFormatter.compact(1500.0))
    }

    @Test
    fun compact_thousands_wholeNumber_dropsDecimal() {
        assertEquals("2k", MoneyFormatter.compact(2000.0))
    }

    @Test
    fun compact_millions_showsMSuffix() {
        assertEquals("1.2m", MoneyFormatter.compact(1_200_000.0))
    }

    @Test
    fun compact_negativeAmount_keepsSign() {
        assertEquals("-1.5k", MoneyFormatter.compact(-1500.0))
    }

    // ---- other currencies

    @Test
    fun format_symbolBeforeTheNumber() {
        assertEquals("$1,234.50", MoneyFormatter.format("USD", 1234.5))
        assertEquals("€1,234.50", MoneyFormatter.format("EUR", 1234.5))
    }

    @Test
    fun format_symbolAfterTheNumber_withSpaceAndCommaDecimals() {
        assertEquals("1\u00A0234,50 Kč", MoneyFormatter.format("CZK", 1234.5))
        assertEquals("1\u00A0234,50 zł", MoneyFormatter.format("PLN", 1234.5))
    }

    @Test
    fun format_dotThousandsAndCommaDecimals() {
        assertEquals("R$1.234,50", MoneyFormatter.format("BRL", 1234.5))
    }

    @Test
    fun format_currenciesWithNoDecimals() {
        assertEquals("¥1,235", MoneyFormatter.format("JPY", 1234.5))
        assertEquals("₩1,235", MoneyFormatter.format("KRW", 1234.5))
        assertEquals("1.234.567₫", MoneyFormatter.format("VND", 1234567.0))
    }

    @Test
    fun format_unknownOrMissingCode_fallsBackToTheRand() {
        assertEquals("R10.00", MoneyFormatter.format("XXX", 10.0))
        assertEquals("R10.00", MoneyFormatter.format("", 10.0))
    }

    @Test
    fun formatSigned_expense_inOtherCurrencies_hasOneMinusInFront() {
        assertEquals("-$100.00", MoneyFormatter.formatSigned("USD", 100.0, isIncome = false))
        assertEquals("-100,00 Kč", MoneyFormatter.formatSigned("CZK", 100.0, isIncome = false))
    }

    @Test
    fun everyCurrency_hasAUniqueCode_andFormatsZeroWithoutThrowing() {
        val codes = MoneyFormatter.AppCurrency.entries.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
        MoneyFormatter.AppCurrency.entries.forEach { assertEquals(true, MoneyFormatter.format(it.code, 0.0).contains(it.symbol)) }
    }

    @Test
    fun withNoOneLoggedIn_theShortFormsUseTheRand() {
        assertEquals("R5.00", MoneyFormatter.format(5.0))
        assertEquals("-R5.00", MoneyFormatter.formatSigned(5.0, isIncome = false))
    }

    @Test
    fun compact_usesTheCurrencysDecimalSeparator() {
        assertEquals("1,5k", MoneyFormatter.compact("CZK", 1500.0))
        assertEquals("1,2m", MoneyFormatter.compact("BRL", 1_200_000.0))
        assertEquals("1.5k", MoneyFormatter.compact("USD", 1500.0))
        assertEquals("-1,5k", MoneyFormatter.compact("PLN", -1500.0))
    }

    @Test
    fun compact_wholeNumbersHaveNoSeparator() {
        assertEquals("2k", MoneyFormatter.compact("CZK", 2000.0))
    }
}
