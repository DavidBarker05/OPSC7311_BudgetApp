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
}
