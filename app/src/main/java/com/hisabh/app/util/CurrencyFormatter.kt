package com.hisabh.app.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    private val indianLocale = Locale("en", "IN")

    fun formatRupees(amount: Double): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(indianLocale)
            format.maximumFractionDigits = 2
            format.minimumFractionDigits = 2
            format.format(amount)
        } catch (_: Exception) {
            String.format(Locale.US, "₹%.2f", amount)
        }
    }

    fun formatNumber(amount: Double): String {
        return try {
            val format = NumberFormat.getNumberInstance(indianLocale)
            format.maximumFractionDigits = 2
            format.minimumFractionDigits = 2
            format.format(amount)
        } catch (_: Exception) {
            String.format(Locale.US, "%.2f", amount)
        }
    }
}

