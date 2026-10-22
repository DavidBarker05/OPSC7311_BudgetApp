package com.example.mybudgettree

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {
    /**
     * A currency the app can show amounts in. Everything is written down here rather than taken from the device's
     * locale data, so an amount reads the same on every phone (and in the unit tests)
     *
     * @property code The ISO 4217 code, which is what is stored on the user
     * @property symbol The symbol shown with an amount
     * @property fmt Where the symbol goes: `%1$s` is the symbol and `%2$s` is the number
     * @property decimals How many decimal places the currency uses
     * @property groupSeparator What separates the thousands
     * @property decimalSeparator What separates the decimals
     */
    enum class AppCurrency(
        val code: String,
        val symbol: String,
        val fmt: String,
        val decimals: Int = 2,
        val groupSeparator: Char = ',',
        val decimalSeparator: Char = '.'
    ) {
        /**
         * Australian dollar
         */
        AUD("AUD", "$", $$"%1$s%2$s"),

        /**
         * Bangladeshi taka
         */
        BDT("BDT", "৳", $$"%1$s%2$s"),

        /**
         * Brazilian real
         */
        BRL("BRL", "R$", $$"%1$s%2$s", groupSeparator = '.', decimalSeparator = ','),

        /**
         * Botswana pula
         */
        BWP("BWP", "P", $$"%1$s%2$s"),

        /**
         * Canadian dollar
         */
        CAD("CAD", "$", $$"%1$s%2$s"),

        /**
         * Czech koruna
         */
        CZK("CZK", "Kč", $$"%2$s %1$s", groupSeparator = '\u00A0', decimalSeparator = ','),

        /**
         * Euro
         */
        EUR("EUR", "€", $$"%1$s%2$s"),

        /**
         * Pound sterling
         */
        GBP("GBP", "£", $$"%1$s%2$s"),

        /**
         * Indian rupee
         */
        INR("INR", "₹", $$"%1$s%2$s"),

        /**
         * Japanese yen
         */
        JPY("JPY", "¥", $$"%1$s%2$s", decimals = 0),

        /**
         * South Korean won
         */
        KRW("KRW", "₩", $$"%1$s%2$s", decimals = 0),

        /**
         * Lesotho loti
         */
        LSL("LSL", "M", $$"%1$s%2$s"),

        /**
         * Namibian dollar
         */
        NAD("NAD", "$", $$"%1$s%2$s"),

        /**
         * Nigerian naira
         */
        NGN("NGN", "₦", $$"%1$s%2$s"),

        /**
         * Pakistani rupee
         */
        PKR("PKR", "Rs", $$"%1$s%2$s"),

        /**
         * Polish złoty
         */
        PLN("PLN", "zł", $$"%2$s %1$s", groupSeparator = '\u00A0', decimalSeparator = ','),

        /**
         * Swazi lilangeni
         */
        SZL("SZL", "E", $$"%1$s%2$s"),

        /**
         * United States dollar
         */
        USD("USD", "$", $$"%1$s%2$s"),

        /**
         * Vietnamese đồng
         */
        VND("VND", "₫", $$"%2$s%1$s", decimals = 0, groupSeparator = '.', decimalSeparator = ','),

        /**
         * South African rand
         */
        ZAR("ZAR", "R", $$"%1$s%2$s");

        /**
         * @param amount The number already written out, without the symbol
         * @return The number with the symbol placed the way this currency does it
         */
        fun format(amount: String): String = fmt.format(symbol, amount)

        /**
         * @param amount The amount, which is made positive
         * @return The number written the way this currency does it, with no symbol
         */
        fun formatNumber(amount: Double): String {
            val symbols = DecimalFormatSymbols(Locale.US).apply {
                groupingSeparator = groupSeparator
                decimalSeparator = this@AppCurrency.decimalSeparator
            }
            val pattern = if (decimals == 0) "#,##0" else "#,##0." + "0".repeat(decimals)
            return DecimalFormat(pattern, symbols).apply { roundingMode = RoundingMode.HALF_UP }.format(amount)
        }

        companion object {
            fun fromCode(code: String?): AppCurrency = entries.firstOrNull { it.code == code } ?: ZAR
        }
    }

    /** The currency of the logged in user, or the rand if no one is logged in */
    private val sessionCurrency: String
        get() = UserSession.currentUser?.currency ?: AppCurrency.ZAR.code

    /**
     * @param currencyCode The ISO code of the currency to show the amount in
     * @param amount The amount to show
     * @return The amount with the currency's symbol, decimals and separators
     */
    fun format(currencyCode: String, amount: Double): String {
        val currency = AppCurrency.fromCode(currencyCode)
        return currency.format(currency.formatNumber(amount))
    }

    /** Same as [format], in the logged in user's currency */
    fun format(amount: Double): String = format(sessionCurrency, amount)

    /**
     * @param currencyCode The ISO code of the currency to show the amount in
     * @param amount The amount to show, whichever sign it has
     * @param isIncome Whether it is income, which has no sign. Anything else gets a minus
     */
    fun formatSigned(currencyCode: String, amount: Double, isIncome: Boolean): String {
        val currency = AppCurrency.fromCode(currencyCode)
        val body = currency.format(currency.formatNumber(kotlin.math.abs(amount)))
        return if (isIncome) body else "-$body"
    }

    /** Same as [formatSigned], in the logged in user's currency */
    fun formatSigned(amount: Double, isIncome: Boolean): String = formatSigned(sessionCurrency, amount, isIncome)

    /**
     * A short form for chart labels, with no symbol: 1500 is 1.5k and 2000000 is 2m. The decimal separator is the
     * currency's, so it is 1,5k for a currency that writes its decimals with a comma
     *
     * @param currencyCode The ISO code of the currency the amount is in
     * @param amount The amount to shorten
     */
    fun compact(currencyCode: String, amount: Double): String {
        val abs = kotlin.math.abs(amount)
        val separator = AppCurrency.fromCode(currencyCode).decimalSeparator
        return when {
            abs >= 1_000_000 -> formatCompactUnit(amount / 1_000_000.0, "m", separator)
            abs >= 1_000 -> formatCompactUnit(amount / 1_000.0, "k", separator)
            else -> amount.toInt().toString()
        }
    }

    /** Same as [compact], in the logged in user's currency */
    fun compact(amount: Double): String = compact(sessionCurrency, amount)

    private fun formatCompactUnit(value: Double, suffix: String, separator: Char): String {
        return if (kotlin.math.abs(value % 1.0) < 0.05) {
            "${value.toInt()}$suffix"
        } else {
            String.format(Locale.US, "%.1f", value).replace('.', separator) + suffix
        }
    }
}
