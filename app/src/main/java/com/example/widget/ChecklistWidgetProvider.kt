package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.model.DailyChecklistData
import com.example.data.repository.PreferenceRepository
import java.util.Calendar

class ChecklistWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        widgetIds.forEach { manager.updateAppWidget(it, buildViews(context, it)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_ITEM) {
            val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
            val prefs = PreferenceRepository(context)
            val completed = prefs.getDailyChecklistCompletedIds()
            prefs.setDailyChecklistItemCompleted(todayKey(), itemId, itemId !in completed)
            updateAll(context)
        } else if (intent.action == ACTION_REFRESH_WIDGET) {
            updateAll(context)
        }
    }

    private fun buildViews(context: Context, widgetId: Int): RemoteViews {
        val completed = PreferenceRepository(context).getDailyChecklistCompletedIds()
        val total = DailyChecklistData.items.size
        return RemoteViews(context.packageName, R.layout.widget_daily_checklist).apply {
            setTextViewText(R.id.widget_title, WidgetTypography.vazirmatn(context, "چک‌لیست امروز", bold = true))
            setTextViewText(R.id.widget_subtitle, WidgetTypography.vazirmatn(context, "قدم‌های کوچک، استمرار زیبا"))
            setTextViewText(
                R.id.widget_progress_text,
                WidgetTypography.vazirmatn(context, "${completed.size.toPersianDigits()} از ${total.toPersianDigits()}", bold = true)
            )
            setTextViewText(R.id.widget_empty_text, WidgetTypography.vazirmatn(context, "در حال آماده‌سازی فهرست…"))
            setTextViewText(R.id.widget_open_button, WidgetTypography.vazirmatn(context, "مشاهده همه اعمال  ←", bold = true))
            setProgressBar(R.id.widget_progress, total, completed.size, false)
            setOnClickPendingIntent(R.id.widget_header, openChecklistIntent(context))
            setOnClickPendingIntent(R.id.widget_open_button, openChecklistIntent(context))
            setRemoteAdapter(
                R.id.widget_task_list,
                Intent(context, ChecklistWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
            )
            setPendingIntentTemplate(R.id.widget_task_list, toggleTemplate(context, widgetId))
            setEmptyView(R.id.widget_task_list, R.id.widget_empty_text)
        }
    }

    private fun openChecklistIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        7000,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_CHECKLIST, true)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun toggleTemplate(context: Context, widgetId: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            8000 + widgetId,
            Intent(context, ChecklistWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_ITEM
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        const val EXTRA_OPEN_CHECKLIST = "OPEN_DAILY_CHECKLIST"
        internal const val ACTION_TOGGLE_ITEM = "ir.adhkar.app.action.TOGGLE_CHECKLIST_WIDGET_ITEM"
        internal const val ACTION_REFRESH_WIDGET = "ir.adhkar.app.action.REFRESH_CHECKLIST_WIDGET"
        internal const val EXTRA_ITEM_ID = "CHECKLIST_ITEM_ID"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, ChecklistWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_task_list)
                val provider = ChecklistWidgetProvider()
                ids.forEach { id -> manager.updateAppWidget(id, provider.buildViews(context, id)) }
            }
        }

        private fun todayKey(): Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        private fun Int.toPersianDigits(): String = toString().map { char ->
            if (char.isDigit()) "۰۱۲۳۴۵۶۷۸۹"[char - '0'] else char
        }.joinToString("")
    }
}
