package com.avioiva.financiiea.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Supported currencies — BDT is the default. */
object Currencies {
    val DEFAULT = "BDT"
    val codes = listOf(
        "BDT", "USD", "EUR", "GBP", "INR", "PKR", "SAR", "AED",
        "CNY", "JPY", "MYR", "SGD", "CAD", "AUD", "RUB"
    )
    private val symbols = mapOf(
        "BDT" to "৳", "USD" to "$", "EUR" to "€", "GBP" to "£",
        "INR" to "₹", "PKR" to "Rs", "SAR" to "SR", "AED" to "AED",
        "CNY" to "¥", "JPY" to "¥", "MYR" to "RM", "SGD" to "S$",
        "CAD" to "C$", "AUD" to "A$", "RUB" to "₽"
    )

    fun symbol(code: String): String = symbols[code] ?: code
}

object Formats {

    private val amountFormat = DecimalFormat("#,##0.00")
    private val amountNoDecimals = DecimalFormat("#,##0.##")
    private val dayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val dayTimeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
    private val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.US)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMM yy", Locale.US)

    /** "12,500.50" */
    fun amount(value: Double): String = amountFormat.format(value)

    /** "৳ 12,500.50" */
    fun money(value: Double, currency: String, withSymbol: Boolean = true): String =
        if (withSymbol) "${symbolOf(currency)} ${amountFormat.format(value)}" else amountFormat.format(value)

    /** "৳ 1,250" (no forced decimals) */
    fun moneyShort(value: Double, currency: String): String =
        "${symbolOf(currency)} ${amountNoDecimals.format(value)}"

    private fun symbolOf(code: String): String = Currencies.symbol(code)

    fun day(timestamp: Long): String = dayFormat.format(Date(timestamp))
    fun dayTime(timestamp: Long): String = dayTimeFormat.format(Date(timestamp))

    fun hourMinute(hour: Int, minute: Int): String =
        String.format(Locale.US, "%02d:%02d", hour, minute)

    fun monthLabel(month: YearMonth): String = month.format(monthYearFormatter)
    fun monthShort(month: YearMonth): String = month.format(monthFormatter)

    // ---- time ranges (epoch millis) ----

    fun zone(): ZoneId = ZoneId.systemDefault()

    fun startOfDay(d: LocalDate = LocalDate.now()): Long =
        d.atStartOfDay(zone()).toInstant().toEpochMilli()

    fun endOfDay(d: LocalDate = LocalDate.now()): Long =
        d.plusDays(1).atStartOfDay(zone()).toInstant().toEpochMilli() - 1

    /** Trailing 7 days including today. */
    fun last7Days(): Pair<Long, Long> =
        startOfDay(LocalDate.now().minusDays(6)) to endOfDay()

    fun currentMonth(): Pair<Long, Long> {
        val m = YearMonth.now()
        return startOfDay(m.atDay(1)) to endOfDay(m.atEndOfMonth())
    }

    fun monthRange(m: YearMonth): Pair<Long, Long> =
        startOfDay(m.atDay(1)) to endOfDay(m.atEndOfMonth())

    fun currentYear(): Pair<Long, Long> {
        val y = LocalDate.now().year
        return startOfDay(LocalDate.of(y, 1, 1)) to endOfDay(LocalDate.of(y, 12, 31))
    }

    fun startOfWeek(): Long =
        startOfDay(LocalDate.now().with(DayOfWeek.MONDAY))

    /** Local date-time from an epoch-millis value. */
    fun toLocalDateTime(timestamp: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zone())

    fun millisFrom(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(zone()).toInstant().toEpochMilli()

    /** Month of a timestamp. */
    fun monthOf(timestamp: Long): YearMonth =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zone()).let { YearMonth.of(it.year, it.monthValue) }

    fun calendarField(timestamp: Long, field: Int): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(field)
    }
}
