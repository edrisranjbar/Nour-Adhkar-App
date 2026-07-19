package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.AdhkarData
import com.example.data.repository.PreferenceRepository

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferenceRepository(context)
        if (!prefs.isNotificationsEnabled()) return

        val type = intent.getStringExtra("REMINDER_TYPE") ?: "general"
        
        val channelId = "nour_adhkar_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "اذکار نور - یادآوری روزانه",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "کانال ارسال یادآوری‌های اذکار صبحگاه و شامگاه"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val (title, text) = when (type) {
            "morning" -> {
                val dhikr = AdhkarData.adhkarList["morning"]?.randomOrNull()
                Pair(
                    "ذکر صبحگاه ☀️",
                    dhikr?.persianTranslation ?: "روز خود را با یاد خدا و تلاوت اذکار صبحگاه نورانی کنید."
                )
            }
            "evening" -> {
                val dhikr = AdhkarData.adhkarList["evening"]?.randomOrNull()
                Pair(
                    "ذکر شامگاه 🌙",
                    dhikr?.persianTranslation ?: "پایان روز را با یاد پروردگار به آرامش برسانید."
                )
            }
            else -> {
                val dhikr = AdhkarData.adhkarList["daily"]?.randomOrNull()
                Pair(
                    "یاد خدا - نور اذکار ✨",
                    dhikr?.persianTranslation ?: "ألا بذکر الله تطمئن القلوب..."
                )
            }
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (type == "morning") 101 else 102,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder) // temporary fallback or we'll use a standard icon
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(if (type == "morning") 1001 else 1002, notification)
    }
}
