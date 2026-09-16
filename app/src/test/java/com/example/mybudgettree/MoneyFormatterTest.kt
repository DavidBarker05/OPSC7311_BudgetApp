package com.example.mybudgettree

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {

    @Test
    fun format_addsRandPrefixAndTwoDecimals() {
        assertEquals("R1,234.50", MoneyFormatter.format(1234.5))
    }

    @Test
    fun format_zero() {
        assertEquals("R0.00", MoneyFormatter.format(0.0))
    }

    @Test
    fun formatSigned_income_hasNoMinus() {
        assertEquals("R100.00", MoneyFormatter.formatSigned(100.0, isIncome = true))
    }

    @Test
    fun formatSigned_expense_hasMinus() {
        assertEquals("-R100.00", MoneyFormatter.formatSigned(100.0, isIncome = false))
    }

    @Test
    fun formatSigned_expense_negativeInputAmount_stillShowsSingleMinus() {
        // amount is stored positive in practice, but the formatter should be robust to a
        // negative input rather than printing a double minus sign
        assertEquals("-R100.00", MoneyFormatter.formatSigned(-100.0, isIncome = false))
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
}
