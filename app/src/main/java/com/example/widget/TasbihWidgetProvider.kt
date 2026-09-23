package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.repository.PreferenceRepository

class TasbihWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        widgetIds.forEach { id ->
            manager.updateAppWidget(id, buildViews(context, id))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val prefs = PreferenceRepository(context)
        when (intent.action) {
            ACTION_INCREMENT -> {
                prefs.incrementTasbihCount()
                prefs.markActivityToday()
                vibrateIfEnabled(context, prefs)
                updateAll(context)
            }
            ACTION_RESET -> {
                prefs.setTasbihCount(0)
                updateAll(context)
            }
            ACTION_NEXT_DHIKR -> {
                val list = getAllDhikrs(prefs)
                val current = prefs.getSelectedTasbihDhikr()
                val idx = list.indexOf(current).let { if (it < 0) 0 else it }
                val next = list[(idx + 1) % list.size]
                prefs.setSelectedTasbihDhikr(next)
                prefs.setTasbihCount(0)
                updateAll(context)
            }
            ACTION_PREV_DHIKR -> {
                val list = getAllDhikrs(prefs)
                val current = prefs.getSelectedTasbihDhikr()
                val idx = list.indexOf(current).let { if (it < 0) 0 else it }
                val prev = list[(idx - 1 + list.size) % list.size]
                prefs.setSelectedTasbihDhikr(prev)
                prefs.setTasbihCount(0)
                updateAll(context)
            }
            ACTION_REFRESH_WIDGET -> {
                updateAll(context)
            }
        }
    }

    private fun buildViews(context: Context, widgetId: Int): RemoteViews {
        val prefs = PreferenceRepository(context)
        val count = prefs.getTasbihCount()
        val dhikr = prefs.getSelectedTasbihDhikr()

        return RemoteViews(context.packageName, R.layout.widget_tasbih).apply {
            setTextViewText(
                R.id.widget_tasbih_title,
                WidgetTypography.vazirmatn(context, "ذکرشمار", bold = true)
            )
            setTextViewText(
                R.id.widget_tasbih_subtitle,
                WidgetTypography.vazirmatn(context, "تسبیح و اذکار روزانه")
            )
            setTextViewText(
                R.id.widget_tasbih_dhikr_text,
                WidgetTypography.amiri(context, dhikr)
            )
            setTextViewText(
                R.id.widget_tasbih_count_text,
                WidgetTypography.vazirmatn(context, count.toPersianDigits(), bold = true)
            )
            setTextViewText(
                R.id.widget_tasbih_hint_text,
                WidgetTypography.vazirmatn(context, "برای شمارش ضربه بزنید (+۱)")
            )
            setTextViewText(
                R.id.widget_tasbih_reset_text,
                WidgetTypography.vazirmatn(context, "صفر", bold = true)
            )
            setTextViewText(
                R.id.widget_tasbih_open_btn,
                WidgetTypography.vazirmatn(context, "باز کردن ذکرشمار در برنامه  ←", bold = true)
            )

            // Pending Intents
            setOnClickPendingIntent(
                R.id.widget_tasbih_count_btn,
                actionIntent(context, ACTION_INCREMENT, widgetId, 100)
            )
            setOnClickPendingIntent(
                R.id.widget_tasbih_reset_btn,
                actionIntent(context, ACTION_RESET, widgetId, 200)
            )
            setOnClickPendingIntent(
                R.id.widget_tasbih_next_dhikr,
                actionIntent(context, ACTION_NEXT_DHIKR, widgetId, 300)
            )
            setOnClickPendingIntent(
                R.id.widget_tasbih_prev_dhikr,
                actionIntent(context, ACTION_PREV_DHIKR, widgetId, 400)
            )

            val openAppIntent = openTasbihIntent(context)
            setOnClickPendingIntent(R.id.widget_tasbih_header, openAppIntent)
            setOnClickPendingIntent(R.id.widget_tasbih_open_btn, openAppIntent)
        }
    }

    private fun actionIntent(context: Context, action: String, widgetId: Int, offset: Int): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(
            context,
            9000 + widgetId * 10 + offset,
            Intent(context, TasbihWidgetProvider::class.java).apply {
                this.action = action
                setPackage(context.packageName)
            },
            flags
        )
    }

    private fun openTasbihIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        7100,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_TASBIH, true)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun vibrateIfEnabled(context: Context, prefs: PreferenceRepository) {
        if (!prefs.isVibrationEnabled()) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (_: Throwable) {}
    }

    companion object {
        const val EXTRA_OPEN_TASBIH = "OPEN_TASBIH_SCREEN"
        internal const val ACTION_INCREMENT = "ir.adhkar.app.action.INCREMENT_TASBIH_WIDGET"
        internal const val ACTION_RESET = "ir.adhkar.app.action.RESET_TASBIH_WIDGET"
        internal const val ACTION_NEXT_DHIKR = "ir.adhkar.app.action.NEXT_TASBIH_DHIKR_WIDGET"
        internal const val ACTION_PREV_DHIKR = "ir.adhkar.app.action.PREV_TASBIH_DHIKR_WIDGET"
        internal const val ACTION_REFRESH_WIDGET = "ir.adhkar.app.action.REFRESH_TASBIH_WIDGET"

        val defaultDhikrs = listOf(
            "سبحان الله",
            "الحمد لله",
            "لا إله إلا الله",
            "الله أكبر",
            "أستغفر الله",
            "اللهم صل على محمد"
        )

        fun getAllDhikrs(prefs: PreferenceRepository): List<String> {
            val custom = prefs.getCustomDhikr()
            return defaultDhikrs + custom
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TasbihWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val provider = TasbihWidgetProvider()
                ids.forEach { id -> manager.updateAppWidget(id, provider.buildViews(context, id)) }
            }
        }

        private fun Int.toPersianDigits(): String = toString().map { char ->
            if (char.isDigit()) "۰۱۲۳۴۵۶۷۸۹"[char - '0'] else char
        }.joinToString("")
    }
}
