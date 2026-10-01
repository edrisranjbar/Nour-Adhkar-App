package com.example.qaza

/** Sanity limits that keep every number editable but bounded. */
object QazaLimits {
    const val MAX_FASTS = 9_999
    const val RECENT_DATES = 30
}

/**
 * Missed fasts (قضای روزه): how many are owed in total, how many were made up, and the dates of the
 * most recent made-up days. The user enters every number; the app makes no rulings. Every change
 * returns a new state, which keeps the logic easy to test.
 */
data class FastingState(
    val owed: Int = 0,
    val madeUp: Int = 0,
    /** Oldest first; only the latest [QazaLimits.RECENT_DATES] are kept. */
    val recentDates: List<Long> = emptyList()
) {
    val remaining: Int get() = (owed - madeUp).coerceAtLeast(0)
    val hasData: Boolean get() = owed > 0

    /** Marks one fast as made up on [now]; does nothing when none are owed. */
    fun markMadeUp(now: Long): FastingState {
        if (remaining == 0) return this
        return copy(
            madeUp = madeUp + 1,
            recentDates = (recentDates + now).takeLast(QazaLimits.RECENT_DATES)
        )
    }

    /** Takes back the most recent made-up day. */
    fun undoMadeUp(): FastingState {
        if (madeUp == 0) return this
        return copy(madeUp = madeUp - 1, recentDates = recentDates.dropLast(1))
    }

    /**
     * Sets how many fasts are owed in total. The progress already made is kept, but it can never
     * exceed the total: lowering the total below it takes the extra days (and their dates) back.
     */
    fun setTotal(total: Int): FastingState {
        val newOwed = total.coerceIn(0, QazaLimits.MAX_FASTS)
        if (newOwed >= madeUp) return copy(owed = newOwed)
        val excess = madeUp - newOwed
        return copy(
            owed = newOwed,
            madeUp = newOwed,
            recentDates = recentDates.dropLast(excess.coerceAtMost(recentDates.size))
        )
    }
}
