package com.example.notifications
import com.example.ui.language.text

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AdhkarData
import com.example.data.repository.PreferenceRepository

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferenceRepository(context)
        val language = prefs.getAppLanguage()
        if (!prefs.isNotificationsEnabled()) return

        val type = intent.getStringExtra(AdhkarNotificationManager.EXTRA_REMINDER_TYPE) ?: "general"
        val scheduler = AdhkarNotificationManager(context)
        if (intent.action == AdhkarNotificationManager.ACTION_SNOOZE_REMINDER) {
            scheduler.scheduleSnooze(type)
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(notificationId(type))
            return
        }
        if ((type == "morning" || type == "evening") && prefs.isAdhkarCompletedToday(type)) {
            scheduler.scheduleNext(type)
            return
        }

        val channelId = "nour_adhkar_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                language.text("اذکار نور - یادآوری روزانه"),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = language.text("یادآوری اذکار صبحگاه، شامگاه و تلاوت سوره کهف در جمعه")
            }
            notificationManager.createNotificationChannel(channel)
        }

        val (title, text, bigText) = when (type) {
            "morning" -> {
                val dhikr = AdhkarData.adhkarList["morning"]?.randomOrNull()
                val intro = "امروز خود را با تلاوت اذکار مبارک صبحگاه متبرک و نورانی کنید. زمان تلاوت فرا رسیده است:"
                val content = dhikr?.let { if (language.showPersianTranslation) "«${it.arabicText}»\n\nترجمه: ${it.persianTranslation}" else "«${it.arabicText}»" }
                    ?: "روز خود را با یاد خدا و تلاوت اذکار صبحگاه نورانی کنید."
                Triple(
                    "☀️ نسیم صبحگاه: یاد خدا",
                    "زمان قرائت اذکار مبارک صبحگاهی است.",
                    "$intro\n\n$content"
                )
            }
            "evening" -> {
                val dhikr = AdhkarData.adhkarList["evening"]?.randomOrNull()
                val intro = "غروبی سرشار از آرامش با یاد پروردگار مهربان. زمان قرائت اذکار مبارک شامگاه فرا رسیده است:"
                val content = dhikr?.let { if (language.showPersianTranslation) "«${it.arabicText}»\n\nترجمه: ${it.persianTranslation}" else "«${it.arabicText}»" }
                    ?: "پایان روز را با یاد پروردگار به آرامش برسانید."
                Triple(
                    "🌙 نور شامگاه: آرامش دل‌ها",
                    "زمان قرائت اذکار مبارک شامگاهی است.",
                    "$intro\n\n$content"
                )
            }
            "friday_kahf" -> Triple(
                "📖 جمعه با سوره کهف",
                "یادآوری تلاوت سوره مبارکه کهف",
                "امروز جمعه است؛ فرصتی آرام برای تلاوت سوره مبارکه کهف. برای شروع، روی دکمه زیر بزنید."
            )
            else -> {
                val dhikr = AdhkarData.adhkarList["daily"]?.randomOrNull()
                val intro = "دل‌ها با یاد الهی به آرامش حقیقی می‌رسند. یادآوری تلاوت اذکار روزانه:"
                val content = dhikr?.let { if (language.showPersianTranslation) "«${it.arabicText}»\n\nترجمه: ${it.persianTranslation}" else "«${it.arabicText}»" }
                    ?: "ألا بذکر الله تطمئن القلوب..."
                Triple(
                    "✨ اذکار نور: آرامش روزانه",
                    "هم‌اکنون زمان تلاوت اذکار روزانه است.",
                    "$intro\n\n$content"
                )
            }
        }

        val startIntent = if (type == "friday_kahf") {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://quran.com/18"))
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (type == "morning" || type == "evening") {
                    putExtra(AdhkarNotificationManager.EXTRA_OPEN_CATEGORY, type)
                }
            }
        }
        val startPendingIntent = PendingIntent.getActivity(
            context,
            notificationId(type),
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId(type) + 100,
            Intent(context, ReminderReceiver::class.java).apply {
                action = AdhkarNotificationManager.ACTION_SNOOZE_REMINDER
                putExtra(AdhkarNotificationManager.EXTRA_REMINDER_TYPE, type)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_adhkar)
            .setContentTitle(language.text(title))
            .setContentText(language.text(text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText.split("\n\n").joinToString("\n\n") { language.text(it) }))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(startPendingIntent)
            .addAction(0, language.text(if (type == "friday_kahf") "باز کردن سوره" else "شروع"), startPendingIntent)
            .addAction(0, language.text("یک ساعت بعد"), snoozePendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId(type), notification)
        if (type in setOf("morning", "evening", "friday_kahf")) scheduler.scheduleNext(type)
    }

    private fun notificationId(type: String): Int = when (type) {
        "morning" -> 1001
        "evening" -> 1002
        "friday_kahf" -> 1003
        else -> 1004
    }
}
