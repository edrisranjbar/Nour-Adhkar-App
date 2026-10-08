package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.repository.PreferenceRepository
import com.example.ui.language.AppLanguage
import com.example.ui.language.text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PrayerTimesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateAll(context)

    // Before Android 12 the launcher only reports the new size; re-render for it.
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) =
        updateAll(context)

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
            val content = PrayerWidgetContent.from(context, prefs, settings, schedule)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // The launcher switches between these as the widget is resized, with no app round-trip.
                val responsive = RemoteViews(SIZES.associate { SizeF(it.widthDp, it.heightDp) to build(context, content, it) })
                manager.updateAppWidget(ids, responsive)
            } else {
                ids.forEach { id -> manager.updateAppWidget(id, build(context, content, sizeFor(manager.getAppWidgetOptions(id)))) }
            }

            val alarms = context.getSystemService(AlarmManager::class.java)
            if (schedule != null) {
                // A non-wakeup, inexact refresh needs no exact-alarm access and never plays audio.
                alarms.set(AlarmManager.RTC, schedule.refreshAt, refreshIntent(context))
            } else alarms.cancel(refreshIntent(context))
        }

        private enum class Shape(val layout: Int, val columns: Int) {
            /** One cell tall: the next prayer only. */
            COMPACT(R.layout.widget_prayer_times_compact, 0),
            /** About 2x2: three rows of two times. */
            GRID(R.layout.widget_prayer_times, 2),
            /** About 4x2: two rows of three times with a side-by-side header. */
            WIDE(R.layout.widget_prayer_times_wide, 3)
        }

        /** The smallest size (dp) a shape needs, and the text scale it uses there. */
        private class WidgetSize(val widthDp: Float, val heightDp: Float, val shape: Shape, val scale: Float)

        private val SIZES = listOf(
            WidgetSize(110f, 40f, Shape.COMPACT, 1f),
            WidgetSize(110f, 110f, Shape.GRID, 1f),
            WidgetSize(140f, 190f, Shape.GRID, 1.25f),
            WidgetSize(250f, 110f, Shape.WIDE, 1f),
            WidgetSize(250f, 170f, Shape.WIDE, 1.25f)
        )

        /** Pre-Android 12: the largest size that fits what the launcher reports (portrait width/height). */
        private fun sizeFor(options: Bundle): WidgetSize = sizeFor(
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        )

        private fun sizeFor(widthDp: Int, heightDp: Int): WidgetSize {
            if (widthDp <= 0 || heightDp <= 0) return SIZES[1]
            return SIZES.filter { it.widthDp <= widthDp && it.heightDp <= heightDp }
                .maxByOrNull { it.widthDp * it.heightDp } ?: SIZES.first()
        }

        /** "COMPACT", "GRID" or "WIDE" plus the text scale, for tests. */
        internal fun describeSize(widthDp: Int, heightDp: Int): String =
            sizeFor(widthDp, heightDp).let { "${it.shape} ${it.scale}" }

        private fun build(context: Context, content: PrayerWidgetContent, size: WidgetSize): RemoteViews =
            RemoteViews(context.packageName, size.shape.layout).apply {
                val colors = content.colors
                setInt(R.id.prayer_widget_root, "setBackgroundResource",
                    if (content.dark) R.drawable.bg_prayer_widget_dark else R.drawable.bg_prayer_widget)
                fun text(id: Int, value: String, color: Int, bold: Boolean, sizeSp: Float) {
                    setTextViewText(id, WidgetTypography.vazirmatn(context, value, bold))
                    setTextColor(id, color)
                    setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, sizeSp * size.scale)
                }
                if (size.shape == Shape.COMPACT) {
                    text(R.id.prayer_widget_city, "${content.title} · ${content.place}", colors.muted, false, 11f)
                    text(R.id.prayer_widget_next, content.nextName ?: content.status, colors.accent, true, 16f)
                    text(R.id.prayer_widget_next_time, content.nextTime.orEmpty(), colors.foreground, true, 22f)
                } else {
                    text(R.id.prayer_widget_title, content.title, colors.foreground, true, 14f)
                    text(R.id.prayer_widget_city, content.place, colors.muted, false, 11f)
                    text(R.id.prayer_widget_next, content.nextLine, colors.accent, true, if (size.shape == Shape.WIDE) 14f else 13f)
                    setViewVisibility(R.id.prayer_widget_grid, if (content.times.isEmpty()) View.GONE else View.VISIBLE)
                    val rows = listOf(R.id.prayer_widget_row1, R.id.prayer_widget_row2, R.id.prayer_widget_row3)
                    rows.forEach { removeAllViews(it) }
                    content.times.forEachIndexed { index, time ->
                        val cell = RemoteViews(context.packageName, R.layout.widget_prayer_time_cell).apply {
                            setTextViewText(R.id.prayer_cell_label, WidgetTypography.vazirmatn(context, time.label, time.active))
                            setTextViewText(R.id.prayer_cell_time, WidgetTypography.vazirmatn(context, time.time, true))
                            setTextColor(R.id.prayer_cell_label, if (time.active) colors.accent else colors.muted)
                            setTextColor(R.id.prayer_cell_time, if (time.active) colors.accent else colors.foreground)
                            setTextViewTextSize(R.id.prayer_cell_label, TypedValue.COMPLEX_UNIT_SP, 12f * size.scale)
                            setTextViewTextSize(R.id.prayer_cell_time, TypedValue.COMPLEX_UNIT_SP, 15f * size.scale)
                            setInt(R.id.prayer_cell_root, "setBackgroundResource", when {
                                !time.active -> 0
                                content.dark -> R.drawable.bg_prayer_cell_active_dark
                                else -> R.drawable.bg_prayer_cell_active
                            })
                        }
                        addView(rows[index / size.shape.columns], cell)
                    }
                }
                setOnClickPendingIntent(R.id.prayer_widget_root, content.open)
            }
    }
}

private class WidgetColors(val foreground: Int, val muted: Int, val accent: Int)

private class WidgetTime(val label: String, val time: String, val active: Boolean)

/** Everything the widget shows, computed once and rendered into each size. */
private class PrayerWidgetContent(
    val dark: Boolean,
    val colors: WidgetColors,
    val title: String,
    val place: String,
    /** Setup or unavailable message when there is no next prayer. */
    val status: String,
    /** «بعدی: ظهر ۱۲:۰۵» for the grid shapes. */
    val nextLine: String,
    /** Next prayer name (with «فردا» when it is tomorrow's) and time, for the compact shape. */
    val nextName: String?,
    val nextTime: String?,
    val times: List<WidgetTime>,
    val open: PendingIntent
) {
    companion object {
        fun from(
            context: Context,
            prefs: PreferenceRepository,
            settings: com.example.prayer.PrayerSettings,
            schedule: PrayerWidgetSchedule?
        ): PrayerWidgetContent {
            val language = prefs.getAppLanguage()
            val arabic = language == AppLanguage.ARABIC
            // Arabic wording is written here; other languages localize the Persian source (Dari, Urdu).
            fun pick(arabicText: String, persian: String) = if (arabic) arabicText else language.text(persian)
            val dark = prefs.isDarkModeEnabled()
            val colors = WidgetColors(
                foreground = Color.parseColor(if (dark) "#E2E3DF" else "#191C1A"),
                muted = Color.parseColor(if (dark) "#BDC9BF" else "#43493F"),
                accent = Color.parseColor(if (dark) "#A3D899" else "#3A6931")
            )
            fun digits(value: String) = value.map {
                if (it in '0'..'9') (if (arabic) "٠١٢٣٤٥٦٧٨٩" else "۰۱۲۳۴۵۶۷۸۹")[it - '0'] else it
            }.joinToString("")
            val formatter = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone(settings.zone) }
            fun time(date: Date?) = date?.let { digits(formatter.format(it)) } ?: "—"
            val labels = if (arabic) listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
                else listOf("صبح", "طلوع", "ظهر", "عصر", "مغرب", "عشاء").map { language.text(it) }

            val placeName = if (settings.isValid()) prefs.getActivePlaceName() else null
            val next = schedule?.next
            val nextName = if (schedule != null && next != null) {
                val label = labels.getOrNull(schedule.today.indexOfFirst { row -> row.first == next.first })
                label?.let { it + if (schedule.tomorrow) " " + pick("غداً", "فردا") else "" }
            } else null
            val status = when {
                !settings.isValid() -> pick("اضغط لتحديد موقعك", "برای تعیین موقعیت بزنید")
                else -> pick("المواقيت غير متاحة", "اوقات در دسترس نیست")
            }
            val nextTime = next?.let { time(it.second) }
            val nextLine = if (nextName != null && nextTime != null) {
                pick("التالي: $nextName $nextTime", "بعدی: $nextName $nextTime")
            } else status
            val times = if (schedule == null) emptyList() else schedule.today.mapIndexed { index, entry ->
                WidgetTime(labels[index], time(entry.second), !schedule.tomorrow && next?.first == entry.first)
            }
            val open = PendingIntent.getActivity(context, 9101,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(PrayerTimesWidgetProvider.EXTRA_OPEN_PRAYERS, true)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            return PrayerWidgetContent(
                dark = dark,
                colors = colors,
                title = pick("مواقيت الصلاة", "اوقات شرعی"),
                place = (placeName ?: settings.location).ifBlank { pick("تحديد الموقع", "تعیین موقعیت") },
                status = status,
                nextLine = nextLine,
                nextName = nextName,
                nextTime = nextTime,
                times = times,
                open = open
            )
        }
    }
}
