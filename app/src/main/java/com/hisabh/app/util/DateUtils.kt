package com.hisabh.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale.ENGLISH)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)

    fun formatMonthYear(calendar: Calendar): String {
        return monthYearFormat.format(calendar.time)
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun getStartOfMonth(calendar: Calendar): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfMonth(calendar: Calendar): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun getDateGroupHeader(timestamp: Long): String {
        val now = Calendar.getInstance()
        val txCal = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isSameYear = now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
        val isSameDay = isSameYear && now.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) {
            return "Today, " + shortDateFormat.format(Date(timestamp))
        }

        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) {
            return "Yesterday, " + shortDateFormat.format(Date(timestamp))
        }

        return dateFormat.format(Date(timestamp))
    }
}

