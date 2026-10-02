package com.example.calendar

data class CalendarDay(
    val jdn: Int,
    val jalali: JalaliDate,
    val hijri: HijriDate,
    val gregorian: GregorianDate,
    val weekday: Int,
    val occasions: List<Occasion>
) {
    val isFriday: Boolean get() = weekday == Weekday.FRIDAY
}

/** One occasion in a month's list; multi-day occasions (e.g. ایام تشریق) appear once with a range. */
data class MonthOccasion(val occasion: Occasion, val first: CalendarDay, val last: CalendarDay)

/**
 * A Jalali month laid out Saturday-first. [leadingBlanks] empty cells come before day 1, so the
 * grid is `leadingBlanks + days.size` cells; the UI always draws six rows to keep its height stable.
 */
data class CalendarMonth(
    val year: Int,
    val month: Int,
    val leadingBlanks: Int,
    val days: List<CalendarDay>
) {
    val occasions: List<MonthOccasion> by lazy {
        val result = mutableListOf<MonthOccasion>()
        days.forEach { day ->
            day.occasions.forEach { occasion ->
                val previous = result.lastOrNull { it.occasion.id == occasion.id }
                if (previous != null && previous.last.jdn == day.jdn - 1) {
                    result[result.indexOf(previous)] = previous.copy(last = day)
                } else {
                    result += MonthOccasion(occasion, day, day)
                }
            }
        }
        result
    }

    /** Hijri months this Jalali month spans, e.g. «ربیع‌الاول – ربیع‌الثانی ۱۴۴۸». */
    val hijriSpan: String get() = span(
        days.first().hijri.let { Triple(it.year, it.month, Hijri.monthNames[it.month - 1]) },
        days.last().hijri.let { Triple(it.year, it.month, Hijri.monthNames[it.month - 1]) }
    )

    /** Gregorian months this Jalali month spans, e.g. «سپتامبر – اکتبر ۲۰۲۶». */
    val gregorianSpan: String get() = span(
        days.first().gregorian.let { Triple(it.year, it.month, Gregorian.monthNames[it.month - 1]) },
        days.last().gregorian.let { Triple(it.year, it.month, Gregorian.monthNames[it.month - 1]) }
    )

    fun dayOf(jdn: Int): CalendarDay? = days.getOrNull(jdn - days.first().jdn)

    companion object {
        fun build(year: Int, month: Int, hijriOffset: Int = 0): CalendarMonth {
            val firstJdn = Jalali.toJdn(year, month, 1)
            val days = List(Jalali.monthLength(year, month)) { index ->
                val jdn = firstJdn + index
                val jalali = JalaliDate(year, month, index + 1)
                val hijri = Hijri.fromJdn(jdn, hijriOffset)
                CalendarDay(
                    jdn = jdn,
                    jalali = jalali,
                    hijri = hijri,
                    gregorian = Gregorian.fromJdn(jdn),
                    weekday = Weekday.indexOf(jdn),
                    occasions = Occasions.on(jalali, hijri)
                )
            }
            return CalendarMonth(year, month, Weekday.indexOf(firstJdn), days)
        }

        private fun span(first: Triple<Int, Int, String>, last: Triple<Int, Int, String>): String = when {
            first.first == last.first && first.second == last.second -> "${first.third} ${first.first}"
            first.first == last.first -> "${first.third} – ${last.third} ${last.first}"
            else -> "${first.third} ${first.first} – ${last.third} ${last.first}"
        }
    }
}
