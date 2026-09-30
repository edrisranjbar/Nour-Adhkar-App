package com.example.notifications

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** Read-only checks and settings shortcuts for "will my reminders actually show up?". */
object ReminderHealth {
    const val REMINDER_CHANNEL_ID = "nour_adhkar_reminders"

    /** False when notifications are off for the app or the reminder channel is blocked. */
    fun notificationsAllowed(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(REMINDER_CHANNEL_ID)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            appDetailsIntent(context)
        }
        start(context, intent, fallback = appDetailsIntent(context))
    }

    fun openAppDetails(context: Context) = start(context, appDetailsIntent(context), fallback = null)

    private fun appDetailsIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    private fun start(context: Context, intent: Intent, fallback: Intent?) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            fallback?.let { start(context, it, fallback = null) }
        } catch (_: SecurityException) {
        }
    }
}
