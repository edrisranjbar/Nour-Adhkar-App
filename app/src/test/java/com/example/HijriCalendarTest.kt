package com.example

import com.example.calendar.CalendarMonth
import com.example.calendar.Gregorian
import com.example.calendar.Hijri
import com.example.calendar.HijriDate
import com.example.calendar.Jalali
import com.example.calendar.JalaliDate
import com.example.calendar.Occasions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/** Hijri dates come from Android's ICU, so these run on Robolectric's Android runtime. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HijriCalendarTest {
    private fun jdn(year: Int, month: Int, day: Int) = Gregorian.toJdn(year, month, day)

    @Test
    fun `matches the JDK's Umm al-Qura calendar for every day 2020 to 2035`() {
        var date = LocalDate.of(2020, 1, 1)
        while (date.year <= 2035) {
            val expected = HijrahDate.from(date)
            val actual = Hijri.fromJdn(jdn(date.year, date.monthValue, date.dayOfMonth))
            assertEquals(
                date.toString(),
                HijriDate(
                    expected.get(ChronoField.YEAR),
                    expected.get(ChronoField.MONTH_OF_YEAR),
                    expected.get(ChronoField.DAY_OF_MONTH)
                ),
                actual
            )
            date = date.plusDays(1)
        }
    }

    @Test
    fun `offset shifts the Hijri date by whole days`() {
        val day = jdn(2026, 10, 2)
        assertEquals(Hijri.fromJdn(day + 1), Hijri.fromJdn(day, offsetDays = 1))
        assertEquals(Hijri.fromJdn(day - 2), Hijri.fromJdn(day, offsetDays = -2))
    }

    @Test
    fun `month grid starts on the right weekday and has every day`() {
        // 1 Farvardin 1405 (2026-03-21) is a Saturday; 1 Mehr 1405 (2026-09-23) is a Wednesday.
        val farvardin = CalendarMonth.build(1405, 1)
        assertEquals(0, farvardin.leadingBlanks)
        assertEquals(31, farvardin.days.size)
        val mehr = CalendarMonth.build(1405, 7)
        assertEquals(4, mehr.leadingBlanks)
        assertEquals(30, mehr.days.size)
        assertEquals(JalaliDate(1405, 7, 30), mehr.days.last().jalali)
        assertTrue(mehr.leadingBlanks + mehr.days.size <= 42)
        assertEquals(Jalali.toJdn(1405, 7, 10), mehr.dayOf(Jalali.toJdn(1405, 7, 10))?.jdn)
    }

    @Test
    fun `occasions follow the Hijri date and multi-day ones collapse into one entry`() {
        assertEquals(listOf("ashura"), Occasions.on(JalaliDate(1405, 4, 1), HijriDate(1448, 1, 10)).map { it.id })
        assertEquals(listOf("nowruz"), Occasions.on(JalaliDate(1405, 1, 1), HijriDate(1447, 10, 2)).map { it.id })
        assertTrue(Occasions.on(JalaliDate(1405, 4, 2), HijriDate(1448, 1, 11)).isEmpty())
        Occasions.all.filter { it.kind == com.example.calendar.Occasion.Kind.RELIGIOUS }
            .forEach { assertTrue(it.id, !it.source.isNullOrBlank()) }

        // Find the Jalali month containing 12 Dhu al-Hijjah 1447 and check ایام تشریق appears once.
        val month = (1..24).asSequence()
            .map { Jalali.shiftMonth(1404, 7, it) }
            .map { (y, m) -> CalendarMonth.build(y, m) }
            .first { m -> m.days.any { it.hijri == HijriDate(1447, 12, 12) } }
        val tashreeq = month.occasions.filter { it.occasion.id == "tashreeq" }
        assertEquals(1, tashreeq.size)
        assertTrue(tashreeq.single().last.jdn > tashreeq.single().first.jdn)
    }

    @Test
    fun `Hijri offset moves Hijri occasions but not Nowruz`() {
        val plain = CalendarMonth.build(1405, 1)
        val shifted = CalendarMonth.build(1405, 1, hijriOffset = 1)
        assertEquals(
            plain.days.first { o -> o.occasions.any { it.id == "nowruz" } }.jdn,
            shifted.days.first { o -> o.occasions.any { it.id == "nowruz" } }.jdn
        )
        val fitr = plain.days.firstOrNull { o -> o.occasions.any { it.id == "eid-al-fitr" } }
        val fitrShifted = shifted.days.firstOrNull { o -> o.occasions.any { it.id == "eid-al-fitr" } }
        if (fitr != null && fitrShifted != null) assertEquals(fitr.jdn - 1, fitrShifted.jdn)
    }
}
