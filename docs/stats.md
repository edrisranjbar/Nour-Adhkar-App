# Statistics (آمار من)

The «آمار» button on the Home card «فعالیت ۳۰ روز گذشته» opens «آمار من». It's fully offline and stores nothing new.

## Screen

1. **Summary tiles:** current streak (the same value as the Home streak), active days this month, and total tasbih.
2. **روزانه (daily):** bars of tasbih per day for 30 days. A dot above a bar means checklist items were done that day.
   - Tapping a bar shows its date and counts above the chart.
   - Arrows page back and forward in 30-day windows.
3. **ماهانه (monthly):** bars for the last 12 Jalali months.
   - A segmented switch chooses the metric: «روزهای فعال» (default, out of the month's length), «تسبیح», or «چک‌لیست».
4. **کل مسیر (overall):** an area chart of the running total of the chosen metric.
   - Range chips: ۳ ماه، ۶ ماه، ۱ سال، همه.
   - A caption gives the lifetime totals since the first recorded day.
5. **Empty state:** with fewer than three active days, a short message replaces the charts.

The charts read right to left: the newest data is on the left. They use Persian digits, theme colours only, and three dashed gridlines with no legend. They animate in, unless the system's "remove animations" setting is on. Each chart has a TalkBack description.

## Data

- **Source:** `stats/StatsAggregator.kt` (pure, unit-tested) works on Julian Day Numbers from:
  - **Tasbih sessions:** all of them, from `TasbihSessionDao.getAllSessions`. The live flow is capped at 500, so it's not used.
  - **Checklist:** completed items for every stored day, via `PreferenceRepository.getAllChecklistCompletionCounts`.
  - **Active days:** the same rule as the streak: activity day keys, any tasbih session, or dhikr progress.
- **Loading:** `AdhkarViewModel.refreshStats()` builds the input on `Dispatchers.Default` when the page opens.
- **Not included yet:** per-day counts for morning, evening and other adhkar collections. The app keeps only each dhikr's current count, not a history. Including them needs a small daily log, which would fill only from the day it ships.

Charts are hand-drawn on a Compose `Canvas` (`ui/components/StatsCharts.kt`). That means no chart dependency, full control over right-to-left layout and digits, and it stays fast with hundreds of points.
