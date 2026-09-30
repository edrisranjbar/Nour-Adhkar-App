package com.example.streak

import java.util.TimeZone

/**
 * Result of [StreakEngine.compute].
 *
 * @property count days of real activity in the current chain. A day covered by a freeze keeps the
 * chain alive but never adds to the count.
 * @property frozenDays days inside the chain that a weekly freeze covered.
 * @property pendingFreezeDay yesterday, when it was missed and the chain survives only if the user
 * is active today; null otherwise.
 */
data class StreakState(
    val count: Int,
    val todayActive: Boolean,
    val frozenDays: Set<Long>,
    val pendingFreezeDay: Long?
) {
    companion object {
        val Empty = StreakState(0, false, emptySet(), null)
    }
}

/**
 * Consecutive-day streak with one forgiven missed day per week.
 *
 * A single missed day is covered (a "freeze") when all of these hold:
 * - the days on both sides of it have real activity (two missed days in a row always break the
 *   streak, and freezes never chain);
 * - the three days before it all have real activity, so a streak must exist before it is protected;
 * - no other day in the same Saturday-to-Friday week was already covered.
 *
 * Nothing is stored: the result depends only on which days were active, so it cannot drift or be
 * consumed twice, and "active days" elsewhere (achievements, the activity calendar) stay honest.
 * When yesterday was missed and today has no activity yet, the streak is reported as saved but
 * pending: it holds only if the user is active today.
 *
 * Days are local-calendar day numbers from [dayNumber].
 */
object StreakEngine {
    const val MIN_ACTIVE_DAYS_BEFORE_FREEZE = 3
    private const val MILLIS_PER_DAY = 86_400_000L
    // Day 0 (1970-01-01) was a Thursday, so day 2 was the first Saturday.
    private const val FIRST_SATURDAY = 2L
    private const val MAX_DAYS = 20_000

    fun dayNumber(millis: Long, zone: TimeZone = TimeZone.getDefault()): Long =
        Math.floorDiv(millis + zone.getOffset(millis), MILLIS_PER_DAY)

    /** Week index where weeks run Saturday through Friday, as in the app's calendar strip. */
    fun weekOf(day: Long): Long = Math.floorDiv(day - FIRST_SATURDAY, 7L)

    private fun firstDayOfWeek(day: Long): Long = weekOf(day) * 7L + FIRST_SATURDAY

    fun compute(today: Long, isActive: (Long) -> Boolean): StreakState {
        val activeMemo = HashMap<Long, Boolean>()
        fun active(day: Long): Boolean =
            day <= today && activeMemo.getOrPut(day) { isActive(day) }

        val frozenMemo = HashMap<Long, Boolean>()
        fun frozen(day: Long, provisional: Boolean): Boolean {
            if (!provisional) frozenMemo[day]?.let { return it }
            val result = !active(day) &&
                (active(day + 1) || (provisional && day + 1 == today)) &&
                (1..MIN_ACTIVE_DAYS_BEFORE_FREEZE).all { active(day - it) } &&
                (firstDayOfWeek(day) until day).none { frozen(it, provisional = false) }
            if (!provisional) frozenMemo[day] = result
            return result
        }

        val todayActive = active(today)
        var count = 0
        var cursor: Long
        var pending: Long? = null
        when {
            todayActive -> { count = 1; cursor = today }
            active(today - 1) -> { count = 1; cursor = today - 1 }
            frozen(today - 1, provisional = true) -> { pending = today - 1; cursor = today - 1 }
            else -> return StreakState.Empty
        }

        val frozenDays = linkedSetOf<Long>()
        var steps = 0
        while (steps++ < MAX_DAYS) {
            val previous = cursor - 1
            when {
                active(previous) -> { count++; cursor = previous }
                frozen(previous, provisional = false) -> { frozenDays += previous; cursor = previous }
                else -> break
            }
        }
        return StreakState(count, todayActive, frozenDays, pending)
    }
}
