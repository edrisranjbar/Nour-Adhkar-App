package com.example.qaza

import java.util.Date

/** What is owed for one prayer: the total owed so far and how many have been made up. */
data class PrayerDebt(val owed: Int = 0, val madeUp: Int = 0) {
    val remaining: Int get() = (owed - madeUp).coerceAtLeast(0)

    /** Same progress, with [remaining] prayers still owed. */
    fun withRemaining(remaining: Int): PrayerDebt =
        PrayerDebt(owed = madeUp + remaining.coerceIn(0, QazaLimits.MAX_PER_PRAYER), madeUp = madeUp)
}

enum class QazaActionType { MISSED, MADE_UP, EDIT, SETUP }

/** One prayer's counters before and after an action, so the action can be undone exactly. */
data class QazaChange(val prayer: QazaPrayer, val before: PrayerDebt, val after: PrayerDebt)

data class QazaAction(val type: QazaActionType, val time: Long, val changes: List<QazaChange>)

/** A block of missed fasts, for example one Ramadan, with the dates they were made up. */
data class FastEntry(
    val id: Long,
    val count: Int,
    val label: String,
    val reason: String,
    val madeUpDates: List<Long>
) {
    val remaining: Int get() = (count - madeUpDates.size).coerceAtLeast(0)
}

/** Everything the tracker stores. All changes return a new state so they are easy to test. */
data class QazaState(
    val prayers: Map<QazaPrayer, PrayerDebt> = emptyMap(),
    val witrEnabled: Boolean = false,
    /** Sets per day: one set is one prayer of every tracked kind. */
    val dailySets: Int = 1,
    /** Oldest first; capped at [QazaLimits.HISTORY_LIMIT]. */
    val history: List<QazaAction> = emptyList(),
    val fasts: List<FastEntry> = emptyList()
) {
    val activePrayers: List<QazaPrayer>
        get() = QazaPrayer.values().filter { witrEnabled || !it.optional }

    fun debt(prayer: QazaPrayer): PrayerDebt = prayers[prayer] ?: PrayerDebt()

    val totalOwed: Int get() = activePrayers.sumOf { debt(it).owed }
    val totalMadeUp: Int get() = activePrayers.sumOf { debt(it).madeUp }
    val totalRemaining: Int get() = activePrayers.sumOf { debt(it).remaining }
    val hasPrayerData: Boolean get() = prayers.values.any { it.owed > 0 } || history.isNotEmpty()

    val fastsOwed: Int get() = fasts.sumOf { it.count }
    val fastsMadeUp: Int get() = fasts.sumOf { it.madeUpDates.size.coerceAtMost(it.count) }
    val fastsRemaining: Int get() = fasts.sumOf { it.remaining }

    /** The day every prayer would be made up at [dailySets] sets a day, or null with no goal. */
    fun estimatedFinish(today: Long): Date? {
        val longest = activePrayers.maxOfOrNull { debt(it).remaining } ?: 0
        return QazaCalculator.completionDate(longest, dailySets, today)
    }

    // ---- prayers ----

    fun addMissed(prayer: QazaPrayer, amount: Int, now: Long): QazaState {
        val current = debt(prayer)
        return applyChanges(
            QazaActionType.MISSED, now,
            mapOf(prayer to current.withRemaining(current.remaining + amount.coerceAtLeast(1)))
        )
    }

    /** Marks prayers as made up; does nothing when none are owed, so it never goes below zero. */
    fun madeUp(prayer: QazaPrayer, amount: Int, now: Long): QazaState {
        val current = debt(prayer)
        if (current.remaining == 0) return this
        val made = amount.coerceIn(1, current.remaining)
        return applyChanges(QazaActionType.MADE_UP, now, mapOf(prayer to current.copy(madeUp = current.madeUp + made)))
    }

    /** Sets how many are still owed, keeping the progress already made. */
    fun setRemaining(prayer: QazaPrayer, remaining: Int, now: Long): QazaState =
        applyChanges(QazaActionType.EDIT, now, mapOf(prayer to debt(prayer).withRemaining(remaining)))

    /** Replaces the counters with a confirmed setup estimate. */
    fun applyEstimate(estimate: Map<QazaPrayer, Int>, now: Long): QazaState {
        val updated = estimate.mapValues { (_, value) ->
            PrayerDebt(owed = value.coerceIn(0, QazaLimits.MAX_PER_PRAYER), madeUp = 0)
        }
        val withWitr = if (estimate.containsKey(QazaPrayer.WITR)) copy(witrEnabled = true) else this
        return withWitr.applyChanges(QazaActionType.SETUP, now, updated)
    }

    fun undoLast(): QazaState {
        val last = history.lastOrNull() ?: return this
        val restored = prayers + last.changes.associate { it.prayer to it.before }
        return copy(prayers = restored, history = history.dropLast(1))
    }

    fun withWitrEnabled(enabled: Boolean): QazaState = copy(witrEnabled = enabled)

    fun withDailySets(sets: Int): QazaState = copy(dailySets = sets.coerceIn(1, QazaLimits.MAX_DAILY_SETS))

    private fun applyChanges(type: QazaActionType, now: Long, updated: Map<QazaPrayer, PrayerDebt>): QazaState {
        val changes = updated.mapNotNull { (prayer, after) ->
            val before = debt(prayer)
            if (before == after) null else QazaChange(prayer, before, after)
        }
        if (changes.isEmpty()) return this
        return copy(
            prayers = prayers + changes.associate { it.prayer to it.after },
            history = (history + QazaAction(type, now, changes)).takeLast(QazaLimits.HISTORY_LIMIT)
        )
    }

    // ---- fasts ----

    fun addFast(count: Int, label: String, reason: String): QazaState {
        if (count <= 0) return this
        val id = (fasts.maxOfOrNull { it.id } ?: 0L) + 1
        val entry = FastEntry(id, count.coerceAtMost(QazaLimits.MAX_FAST_COUNT), label.trim(), reason.trim(), emptyList())
        return copy(fasts = fasts + entry)
    }

    /** Marks one day of an entry as made up on [now]; does nothing when none remain. */
    fun markFastDay(id: Long, now: Long): QazaState = updateFast(id) { entry ->
        if (entry.remaining == 0) entry else entry.copy(madeUpDates = entry.madeUpDates + now)
    }

    fun unmarkFastDay(id: Long): QazaState = updateFast(id) { entry ->
        entry.copy(madeUpDates = entry.madeUpDates.dropLast(1))
    }

    /** Edits an entry; the count never drops below the days already made up. */
    fun editFast(id: Long, count: Int, label: String, reason: String): QazaState = updateFast(id) { entry ->
        entry.copy(
            count = count.coerceIn(maxOf(1, entry.madeUpDates.size), QazaLimits.MAX_FAST_COUNT),
            label = label.trim(),
            reason = reason.trim()
        )
    }

    fun deleteFast(id: Long): QazaState = copy(fasts = fasts.filterNot { it.id == id })

    private fun updateFast(id: Long, transform: (FastEntry) -> FastEntry): QazaState =
        copy(fasts = fasts.map { if (it.id == id) transform(it) else it })
}
