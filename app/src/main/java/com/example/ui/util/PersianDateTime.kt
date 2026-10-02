package com.example.ui.util

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

private val persianMonthNames = listOf(
    "فروردین",
    "اردیبهشت",
    "خرداد",
    "تیر",
    "مرداد",
    "شهریور",
    "مهر",
    "آبان",
    "آذر",
    "دی",
    "بهمن",
    "اسفند"
)

fun formatPersianDateTime(
    timestamp: Long,
    timeZone: TimeZone = TimeZone.getTimeZone("Asia/Tehran")
): String {
    val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = timestamp }
    val (year, month, day) = gregorianToPersian(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH)
    )
    val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')

    return "$day ${persianMonthNames[month - 1]} $year - $hour:$minute".toPersianDigits()
}

fun formatPersianDate(
    timestamp: Long,
    timeZone: TimeZone = TimeZone.getTimeZone("Asia/Tehran")
): String {
    val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = timestamp }
    val (year, month, day) = gregorianToPersian(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    val weekDay = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SATURDAY -> "شنبه"
        Calendar.SUNDAY -> "یکشنبه"
        Calendar.MONDAY -> "دوشنبه"
        Calendar.TUESDAY -> "سه‌شنبه"
        Calendar.WEDNESDAY -> "چهارشنبه"
        Calendar.THURSDAY -> "پنجشنبه"
        else -> "جمعه"
    }
    return "$weekDay $day ${persianMonthNames[month - 1]} $year".toPersianDigits()
}

/** Formats the server's Gregorian calendar date without shifting it across timezones. */
fun formatInboxDate(serverDate: String): String {
    val date = serverDate.take(10)
    if (!date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) return "تاریخ نامشخص"
    val year = date.substring(0, 4).toInt()
    val month = date.substring(5, 7).toInt()
    val day = date.substring(8, 10).toInt()
    val valid = runCatching {
        GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            isLenient = false
            clear()
            set(year, month - 1, day)
        }.timeInMillis
    }.isSuccess
    if (!valid) return "تاریخ نامشخص"
    val (persianYear, persianMonth, persianDay) = gregorianToPersian(year, month, day)
    return "$persianDay ${persianMonthNames[persianMonth - 1]} $persianYear".toPersianDigits()
}

private fun gregorianToPersian(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
    val daysBeforeMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val leapAdjustedYear = if (month > 2) year + 1 else year
    var days = 355666 +
        (365 * year) +
        ((leapAdjustedYear + 3) / 4) -
        ((leapAdjustedYear + 99) / 100) +
        ((leapAdjustedYear + 399) / 400) +
        day +
        daysBeforeMonth[month - 1]

    var persianYear = -1595 + (33 * (days / 12053))
    days %= 12053
    persianYear += 4 * (days / 1461)
    days %= 1461

    if (days > 365) {
        persianYear += (days - 1) / 365
        days = (days - 1) % 365
    }

    val persianMonth: Int
    val persianDay: Int
    if (days < 186) {
        persianMonth = 1 + (days / 31)
        persianDay = 1 + (days % 31)
    } else {
        persianMonth = 7 + ((days - 186) / 30)
        persianDay = 1 + ((days - 186) % 30)
    }

    return Triple(persianYear, persianMonth, persianDay)
}
