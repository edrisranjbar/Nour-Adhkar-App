package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.share.AppLinks
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text
import com.example.ui.theme.SunGold

/**
 * Decides when to ask for a Cafe Bazaar rating. Only asks users with an active streak, at most
 * three times, a week apart, and never again once they rated or said they were unhappy.
 */
class RatingPromptStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rating_prompt", Context.MODE_PRIVATE)

    fun shouldAsk(streakDays: Int, now: Long = System.currentTimeMillis()): Boolean {
        if (streakDays < MIN_STREAK_DAYS || prefs.getBoolean(KEY_DONE, false)) return false
        if (prefs.getInt(KEY_TIMES_ASKED, 0) >= MAX_ASKS) return false
        return now >= prefs.getLong(KEY_NEXT_ASK_AT, 0L)
    }

    fun markAsked(now: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putInt(KEY_TIMES_ASKED, prefs.getInt(KEY_TIMES_ASKED, 0) + 1)
            .putLong(KEY_NEXT_ASK_AT, now + SNOOZE_MILLIS)
            .apply()
    }

    /** The user rated or sent feedback; don't ask again. */
    fun markDone() {
        prefs.edit().putBoolean(KEY_DONE, true).apply()
    }

    private companion object {
        const val MIN_STREAK_DAYS = 3
        const val MAX_ASKS = 3
        const val SNOOZE_MILLIS = 7L * 24 * 60 * 60 * 1000
        const val KEY_DONE = "done"
        const val KEY_TIMES_ASKED = "times_asked"
        const val KEY_NEXT_ASK_AT = "next_ask_at"
    }
}

/** Opens the Cafe Bazaar rating page, falling back to the web listing when Bazaar is missing. */
fun openBazaarRating(context: Context) {
    val rate = Intent(Intent.ACTION_EDIT, Uri.parse(AppLinks.BAZAAR_DETAILS_URI))
        .setPackage(AppLinks.BAZAAR_PACKAGE)
    try {
        context.startActivity(rate)
    } catch (_: ActivityNotFoundException) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.BAZAAR_WEB_URL))) }
    }
}

private fun openFeedbackEmail(context: Context, subject: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${AppLinks.FEEDBACK_EMAIL}"))
        .putExtra(Intent.EXTRA_SUBJECT, subject)
    runCatching { context.startActivity(intent) }
}

/**
 * Asks whether the user is happy before sending them to Bazaar. Happy users go to the rating
 * page; unhappy users are offered a direct email so their feedback reaches the developer.
 */
@Composable
fun RatingPromptDialog(store: RatingPromptStore, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.Star, contentDescription = null, tint = SunGold) },
            title = { Text("از اذکار نور راضی هستید؟", textAlign = TextAlign.Center) },
            text = {
                Text(
                    "امتیاز و نظر شما در کافه‌بازار کمک می‌کند افراد بیشتری اذکار نور را پیدا کنند.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = {
                            store.markDone()
                            openBazaarRating(context)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("بله، امتیاز می‌دهم") }
                    OutlinedButton(
                        onClick = {
                            store.markDone()
                            openFeedbackEmail(context, language.text("پیشنهاد برای اذکار نور"))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("نه چندان؛ پیشنهاد می‌دهم") }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("بعداً") }
                }
            }
        )
    }
}
