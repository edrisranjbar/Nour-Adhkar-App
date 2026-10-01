# Missed fasts (قضای روزه)

A private, offline counter for fasts the user owes. It only counts the number the user enters: it
makes no religious rulings, has no fidya/kaffara amounts, and does not decide who owes what.

Scope note: issue #33 also proposed a missed-prayer (قضای نماز) tracker. This release is deliberately
limited to fasting, following the repository's «refine, don't expand» rule; missed prayers are a
possible follow-up.

## Entry point

The drawer item «قضای روزه» (`qaza` tab in `MainActivity`), placed right after «مقالات». The bottom
navigation and the daily checklist are unchanged.

## Screen

- **Empty state:** an icon, one sentence explaining the feature and «ثبت تعداد روزه‌های فوت‌شده».
- **Setting the count:** the dialog asks for a single thing, the number of days («تعداد روزها»). The
  number is the **total** of missed fasts, not an amount to add.
- **Summary card:** a large progress ring (made up ÷ total, animated) with the remaining number in the
  centre, or a check mark and «همه‌ی روزه‌ها ادا شد» when nothing remains. Two chips below it show the
  made-up and total counts.
- **«ویرایش تعداد»:** edits the total count (capped at 9,999). Raising it keeps the days already made
  up; lowering it below them takes the extra days (and their dates) back, so progress never exceeds the
  total. Setting it to 0 clears the tracker.
- **«یک روز ادا شد»:** the main button; marks one fast as made up today. It does nothing when none remain.
- **«واگرد آخرین روز»:** takes back the most recent made-up day.
- **«آخرین روزهای ادا‌شده»:** a card with the dates (Persian calendar) of the last five made-up days; the
  last 30 dates are stored. Dates are the stored timestamps and are never recomputed.

## Streak

Making up a fast counts as worship activity for the day (`markActivityToday()`), like finishing a
checklist task. Days already forgiven by the weekly streak freeze are unaffected.

## Storage

`QazaRepository` (`data/repository/`) keeps the state as one JSON document (`owed`, `madeUp`, `dates`)
under the key `state` in its own preferences file `qaza_tracker.xml`. The state is a pure Kotlin model
(`qaza/FastingState.kt`) whose changes return a new state, which keeps it easy to test. Parsing is
tolerant: unknown fields and wrong types are skipped, counters are clamped to stay consistent, and
unreadable or older-format data gives an empty tracker. The Room schema (version 1), permissions and
reminder schedules are untouched.

The file is **not** excluded in `backup_rules.xml` / `data_extraction_rules.xml` (only `account.xml` and
`app_inbox.xml` are), so the counters are included in Android backup and device transfer.

## Code map

| Piece | File |
|---|---|
| Fasting state and limits | `qaza/FastingState.kt` |
| JSON storage | `data/repository/QazaRepository.kt` |
| ViewModel state and actions (`qaza…`) | `ui/viewmodel/AdhkarViewModel.kt` |
| Screen and count dialog | `ui/screens/QazaScreen.kt` |
| Arabic strings | `ui/language/ArabicCatalog.kt` |

## Tests

`FastingStateTest` (setting the total, made up, never below zero, undo, progress kept or trimmed when the
total changes, caps, date limit)
and `QazaRepositoryTest` (persistence across recreation and restart, round trip, tolerant parsing).

## Not included

Missed prayers, fidya/kaffara or any fatwa text, labels and reasons per entry, a Sunnah fasting
calendar, a Ramadan countdown, reminders, widgets and cloud sync. Any future religious wording needs
primary sources and review first.
