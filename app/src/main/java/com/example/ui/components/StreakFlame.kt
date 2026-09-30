package com.example.ui.components

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Rive
import com.example.R
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment

// Contract of res/raw/streak_flame.riv (see tools/rive/streak_flame.py).
private const val FLAME_ARTBOARD = "StreakFlame"
private const val FLAME_STATE_MACHINE = "StreakFlame"
private const val INPUT_CELEBRATE = "celebrate"
private const val INPUT_LEVEL = "level"

/** Flame size/heat for the Rive "level" input: 0 small .. 3 large and hot. */
internal fun streakFlameLevel(streak: Int): Float = when {
    streak >= 30 -> 3f
    streak >= 7 -> 2f
    streak >= 3 -> 1f
    else -> 0f
}

internal object RiveSupport {
    @Volatile
    private var ready: Boolean? = null

    /** Loads Rive's native library once; false if this device can't (the Canvas flame is used). */
    fun ensureInit(context: Context): Boolean = ready ?: synchronized(this) {
        ready ?: runCatching { Rive.init(context.applicationContext) }.isSuccess.also { ready = it }
    }
}

/**
 * The streak flame: a Rive state machine that pops in with squash-and-stretch and a spark ring,
 * then flickers. Tapping replays the burst. Falls back to the Compose Canvas flame if Rive fails.
 */
@Composable
fun StreakFlame(streak: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val riveReady = remember { RiveSupport.ensureInit(context) }
    var failed by remember { mutableStateOf(false) }
    if (!riveReady || failed) {
        AnimatedRealisticFireWithSparks(modifier)
        return
    }

    var riveView by remember { mutableStateOf<RiveAnimationView?>(null) }
    val level = streakFlameLevel(streak)
    LaunchedEffect(riveView, level) {
        riveView?.let { view -> runCatching { view.setNumberState(FLAME_STATE_MACHINE, INPUT_LEVEL, level) } }
    }

    AndroidView(
        modifier = modifier
            .semantics { contentDescription = "شعلهٔ زنجیره" }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                riveView?.let { view ->
                    runCatching { view.fireState(FLAME_STATE_MACHINE, INPUT_CELEBRATE) }
                    view.confirmHaptic()
                }
            },
        factory = { ctx ->
            RiveAnimationView(ctx).also { view ->
                val loaded = runCatching {
                    view.setRiveResource(
                        resId = R.raw.streak_flame,
                        artboardName = FLAME_ARTBOARD,
                        stateMachineName = FLAME_STATE_MACHINE,
                        autoplay = true,
                        fit = Fit.CONTAIN,
                        alignment = RiveAlignment.CENTER
                    )
                }.isSuccess
                if (loaded) riveView = view else failed = true
            }
        },
        onRelease = { riveView = null }
    )
}

internal fun View.confirmHaptic() {
    performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.KEYBOARD_TAP
    )
}
