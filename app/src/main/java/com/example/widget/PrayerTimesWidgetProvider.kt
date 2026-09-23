package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.repository.PreferenceRepository
import com.example.ui.language.AppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PrayerTimesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateAll(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in setOf(ACTION_REFRESH, Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) {
            updateAll(context)
        }
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))
    }

    companion object {
        const val EXTRA_OPEN_PRAYERS = "OPEN_PRAYER_WIDGET_SETTINGS"
        private const val ACTION_REFRESH = "com.example.widget.REFRESH_PRAYER_TIMES"

        private fun refreshIntent(context: Context) = PendingIntent.getBroadcast(context, 9100,
            Intent(context, PrayerTimesWidgetProvider::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PrayerTimesWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val prefs = PreferenceRepository(context)
            val settings = prefs.getPrayerSettings()
            val schedule = PrayerWidgetSchedule.calculate(settings, Date())
            val arabic = prefs.getAppLanguage() == AppLanguage.ARABIC
            val dark = prefs.isDarkModeEnabled()
            val foreground = Color.parseColor(if (dark) "#E2E3DF" else "#191C1A")
            val muted = Color.parseColor(if (dark) "#BDC9BF" else "#43493F")
            val accent = Color.parseColor(if (dark) "#A3D899" else "#3A6931")
            fun digits(value: String) = value.map {
                if (it in '0'..'9') (if (arabic) "٠١٢٣٤٥٦٧٨٩" else "۰۱۲۳۴۵۶۷۸۹")[it - '0'] else it
            }.joinToString("")
            val formatter = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone(settings.zone) }
            fun time(date: Date?) = date?.let { digits(formatter.format(it)) } ?: "—"
            val labels = if (arabic) listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
                else listOf("صبح", "طلوع", "ظهر", "عصر", "مغرب", "عشاء")
            val open = PendingIntent.getActivity(context, 9101,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_OPEN_PRAYERS, true)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val views = RemoteViews(context.packageName, R.layout.widget_prayer_times).apply {
                setInt(R.id.prayer_widget_root, "setBackgroundResource", if (dark) R.drawable.bg_prayer_widget_dark else R.drawable.bg_prayer_widget)
                fun text(id: Int, value: String, color: Int, bold: Boolean = false) {
                    setTextViewText(id, WidgetTypography.vazirmatn(context, value, bold))
                    setTextColor(id, color)
                }
                text(R.id.prayer_widget_title, if (arabic) "مواقيت الصلاة" else "اوقات شرعی", foreground, true)
                text(R.id.prayer_widget_city, settings.location.ifBlank { if (arabic) "تحديد الموقع" else "تعیین موقعیت" }, muted)
                val next = schedule?.next
                val nextLabel = next?.let { labels[schedule.today.indexOfFirst { row -> row.first == it.first }] }
                val nextText = when {
                    !settings.isValid() -> if (arabic) "اضغط لتحديد موقعك" else "برای تعیین موقعیت بزنید"
                    next == null -> if (arabic) "المواقيت غير متاحة" else "اوقات در دسترس نیست"
                    else -> (if (arabic) "التالي: " else "بعدی: ") + nextLabel +
                        (if (schedule.tomorrow) (if (arabic) " غداً" else " فردا") else "") + " " + time(next.second)
                }
                text(R.id.prayer_widget_next, nextText, accent, true)
                setViewVisibility(R.id.prayer_widget_grid, if (schedule == null) View.GONE else View.VISIBLE)
                val rowIds = listOf(R.id.prayer_widget_row1, R.id.prayer_widget_row2, R.id.prayer_widget_row3)
                rowIds.forEach { removeAllViews(it) }
                schedule?.today?.forEachIndexed { index, entry ->
                    val active = !schedule.tomorrow && next?.first == entry.first
                    val cell = RemoteViews(context.packageName, R.layout.widget_prayer_time_cell).apply {
                        setTextViewText(R.id.prayer_cell_label, WidgetTypography.vazirmatn(context, labels[index], active))
                        setTextViewText(R.id.prayer_cell_time, WidgetTypography.vazirmatn(context, time(entry.second), true))
                        setTextColor(R.id.prayer_cell_label, if (active) accent else muted)
                        setTextColor(R.id.prayer_cell_time, if (active) accent else foreground)
                        setInt(R.id.prayer_cell_root, "setBackgroundColor", if (active) Color.parseColor(if (dark) "#263428" else "#E8F0E1") else Color.TRANSPARENT)
                    }
                    addView(rowIds[index / 2], cell)
                }
                setOnClickPendingIntent(R.id.prayer_widget_root, open)
            }
            manager.updateAppWidget(ids, views)
            val alarms = context.getSystemService(AlarmManager::class.java)
            if (schedule != null) {
                // A non-wakeup, inexact refresh needs no exact-alarm access and never plays audio.
                alarms.set(AlarmManager.RTC, schedule.refreshAt, refreshIntent(context))
            } else alarms.cancel(refreshIntent(context))
        }
    }
}
