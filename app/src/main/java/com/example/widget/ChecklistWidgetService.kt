package com.example.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.model.DailyChecklistData
import com.example.data.model.DailyChecklistEntry
import com.example.data.repository.PreferenceRepository

class ChecklistWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = ChecklistWidgetFactory(applicationContext)
}

private class ChecklistWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var completedIds: Set<String> = emptySet()
    private var items: List<DailyChecklistEntry> = emptyList()

    override fun onCreate() = reload()
    override fun onDataSetChanged() = reload()
    override fun onDestroy() = Unit
    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        val item = items.getOrNull(position) ?: return null
        val completed = item.id in completedIds
        return RemoteViews(context.packageName, R.layout.widget_checklist_item).apply {
            setTextViewText(R.id.widget_item_title, WidgetTypography.vazirmatn(context, item.title))
            setImageViewResource(
                R.id.widget_item_status,
                if (completed) R.drawable.ic_widget_checked else R.drawable.ic_widget_unchecked
            )
            setInt(
                R.id.widget_item_title,
                "setTextColor",
                context.getColor(if (completed) R.color.widget_text_completed else R.color.widget_text_primary)
            )
            val fillInIntent = Intent().putExtra(ChecklistWidgetProvider.EXTRA_ITEM_ID, item.id)
            setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)
            setOnClickFillInIntent(R.id.widget_item_status, fillInIntent)
            setOnClickFillInIntent(R.id.widget_item_title, fillInIntent)
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long =
        items.getOrNull(position)?.id?.hashCode()?.toLong()?.let { kotlin.math.abs(it) } ?: position.toLong()
    override fun hasStableIds(): Boolean = true

    private fun reload() {
        completedIds = PreferenceRepository(context).getDailyChecklistCompletedIds()
        items = DailyChecklistData.items.sortedBy { it.id in completedIds }
    }
}
