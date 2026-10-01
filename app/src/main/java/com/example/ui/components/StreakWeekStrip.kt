package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit
import com.example.R
import com.example.ui.language.LocalizedText as Text
import com.example.ui.screens.DayActivity
import com.example.ui.theme.SunGold
import com.example.ui.util.toPersianDigits
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment

// Contract of res/raw/streak_week.riv (see tools/rive/streak_week.py).
private const val WEEK_ARTBOARD = "StreakWeek"
private const val WEEK_STATE_MACHINE = "StreakWeek"
private const val INPUT_RTL = "rtl"
private const val INPUT_FILL_TODAY = "fillToday"
private const val WEEK_DAYS = 7
/** The artboard's "enter" pop-in: first column after this delay, then one every step (tools/rive/streak_week.py). */
internal const val WEEK_ENTER_DELAY_MS = 350L
internal const val WEEK_ENTER_STEP_MS = 170L
/** The artboard is 7 columns of 48 x 56, so it is always drawn at this aspect ratio. */
private const val WEEK_ASPECT = (WEEK_DAYS * 48f) / 56f

/** Per-day state numbers understood by the Rive file's "dayN" inputs. */
internal object WeekDayState {
    const val MISSED = 0
    const val DONE = 1
    const val FROZEN = 2
    const val PENDING = 3
    const val TODAY_EMPTY = 4
    const val TODAY_DONE = 5
}

/** Maps a calendar day to the Rive state. Today is "done" only once the fill animation is due. */
internal fun DayActivity.riveState(todayFilled: Boolean): Int = when {
    isToday -> if (isActive && todayFilled) WeekDayState.TODAY_DONE else WeekDayState.TODAY_EMPTY
    isActive -> WeekDayState.DONE
    isFrozen -> WeekDayState.FROZEN
    isFreezePending -> WeekDayState.PENDING
    else -> WeekDayState.MISSED
}

/**
 * The streak dialog's week strip. The circles (check, freeze snowflake, pending snowflake, today
 * ring, entrance and today's fill) are drawn by Rive. The day letters, the «امروز» caption and the
 * explanatory note stay Compose text so RTL, Arabic, font scaling and TalkBack keep working.
 *
 * [fallback] is the plain Compose strip, shown instead when the system's "remove animations"
 * setting is on or when Rive cannot load on the device.
 */
@Composable
fun StreakWeekSection(
    days: List<DayActivity>,
    todayFilled: Boolean,
    reduceMotion: Boolean,
    fontScale: Float,
    fallback: @Composable () -> Unit
) {
    val context = LocalContext.current
    val riveReady = remember { RiveSupport.ensureInit(context) }
    var failed by remember { mutableStateOf(false) }
    val useRive = riveReady && !failed && !reduceMotion && days.size == WEEK_DAYS

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp)
    ) {
        if (!useRive) {
            fallback()
        } else {
            RiveWeek(days, todayFilled, fontScale, onFailed = { failed = true })
        }
    }
}

@Composable
private fun RiveWeek(days: List<DayActivity>, todayFilled: Boolean, fontScale: Float, onFailed: () -> Unit) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val states = days.map { it.riveState(todayFilled) }
    var riveView by remember { mutableStateOf<RiveAnimationView?>(null) }
    var lastToday by remember { mutableIntStateOf(-1) }

    // Today's fill is a one-shot: fire it only on the step from "not yet" to "done".
    val todayState = states.last()
    LaunchedEffect(riveView, todayState) {
        val view = riveView ?: return@LaunchedEffect
        if (lastToday == WeekDayState.TODAY_EMPTY && todayState == WeekDayState.TODAY_DONE) {
            runCatching { view.fireState(WEEK_STATE_MACHINE, INPUT_FILL_TODAY) }
        }
        lastToday = todayState
    }

    val activeCount = days.count { it.isActive }
    val frozenCount = days.count { it.isFrozen }
    val summary = buildString {
        append("هفت روز اخیر: ")
        append(activeCount.toPersianDigits()).append(" روز فعال")
        if (frozenCount > 0) append("، ").append(frozenCount.toPersianDigits()).append(" روز حفظ‌شده با سپر هفتگی")
    }

    Column {
        // Day letters. Columns match the artboard's 7 equal columns; in RTL the first (oldest) day is
        // on the right in both this row and the artboard ("rtl" input).
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            days.forEach { day ->
                Text(
                    text = day.dayLabel,
                    fontSize = (11.5 * fontScale).sp,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (day.isToday) SunGold else Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(WEEK_ASPECT)
                .semantics { contentDescription = summary },
            factory = { ctx ->
                RiveAnimationView(ctx).also { view ->
                    val loaded = runCatching {
                        view.setRiveResource(
                            resId = R.raw.streak_week,
                            artboardName = WEEK_ARTBOARD,
                            stateMachineName = WEEK_STATE_MACHINE,
                            autoplay = true,
                            fit = Fit.FIT_WIDTH,
                            alignment = RiveAlignment.CENTER
                        )
                    }.isSuccess
                    if (loaded) {
                        applyWeek(view, states, rtl)
                        riveView = view
                    } else {
                        onFailed()
                    }
                }
            },
            update = { view -> applyWeek(view, states, rtl) },
            onRelease = { riveView = null }
        )
        Row(Modifier.fillMaxWidth().padding(top = 2.dp).clearAndSetSemantics { }) {
            days.forEach { day ->
                Text(
                    text = if (day.isToday) "امروز" else "",
                    fontSize = (8.5 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = SunGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun applyWeek(view: RiveAnimationView, states: List<Int>, rtl: Boolean) {
    runCatching {
        states.forEachIndexed { index, state ->
            view.setNumberState(WEEK_STATE_MACHINE, "day$index", state.toFloat())
        }
        view.setNumberState(WEEK_STATE_MACHINE, INPUT_RTL, if (rtl) 1f else 0f)
    }
}
