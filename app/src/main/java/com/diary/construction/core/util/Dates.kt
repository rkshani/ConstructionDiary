package com.diary.construction.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** All dates are epoch millis. Day values are local midnight. */
object Dates {
    const val DAY_MS = 24L * 60 * 60 * 1000

    fun now(): Long = System.currentTimeMillis()

    fun today(): Long = startOfDay(now())

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun endOfDay(millis: Long): Long = startOfDay(millis) + DAY_MS - 1

    fun addDays(dayMillis: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = dayMillis
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

    /** Monday of the week that holds [millis]. */
    fun startOfWeek(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = startOfDay(millis) }
        val dow = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1
        val back = (dow + 5) % 7 // days since Monday
        cal.add(Calendar.DAY_OF_YEAR, -back)
        return cal.timeInMillis
    }

    fun startOfMonth(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = startOfDay(millis)
        set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

    fun dayOfWeek(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)

    fun format(millis: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))

    fun formatShort(millis: Long): String = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(millis))

    fun formatDay(millis: Long): String = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(millis))

    fun fileStamp(millis: Long = now()): String = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(millis))

    fun isoDate(millis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

    /** The Material date picker works in UTC midnight; convert to and from local midnight. */
    fun localToUtcDay(local: Long): Long {
        val local0 = Calendar.getInstance().apply { timeInMillis = local }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local0.get(Calendar.YEAR), local0.get(Calendar.MONTH), local0.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    fun utcToLocalDay(utc: Long): Long {
        val utc0 = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
        return Calendar.getInstance().apply {
            clear()
            set(utc0.get(Calendar.YEAR), utc0.get(Calendar.MONTH), utc0.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    fun daysBetween(fromDay: Long, toDay: Long): Int =
        Math.round((startOfDay(toDay) - startOfDay(fromDay)).toDouble() / DAY_MS).toInt()
}
