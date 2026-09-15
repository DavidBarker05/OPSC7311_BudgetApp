package com.example.mybudgettree

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {
    private val amountFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    fun format(amount: Double): String = "R${amountFormat.format(amount)}"

    fun formatSigned(amount: Double, isIncome: Boolean): String {
        val body = amountFormat.format(kotlin.math.abs(amount))
        return if (isIncome) "R$body" else "-R$body"
    }

    fun compact(amount: Double): String {
        val abs = kotlin.math.abs(amount)
        return when {
            abs >= 1_000_000 -> formatCompactUnit(amount / 1_000_000.0, "m")
            abs >= 1_000 -> formatCompactUnit(amount / 1_000.0, "k")
            else -> amount.toInt().toString()
        }
    }

    private fun formatCompactUnit(value: Double, suffix: String): String {
        return if (kotlin.math.abs(value % 1.0) < 0.05) {
            "${value.toInt()}$suffix"
        } else {
            String.format(Locale.US, "%.1f%s", value, suffix)
        }
    }
}
