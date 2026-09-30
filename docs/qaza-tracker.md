# Missed prayers and fasts (قضای نماز و روزه)

A private, offline tracker for prayers and fasts the user owes. It only counts the numbers the user
enters: it makes no religious rulings, has no fidya/kaffara amounts, and does not decide who owes
what. Counting differences (for example Witr, or the age prayer became obligatory) are a setting or a
free input, never a hard-coded assumption.

## Entry points

- Drawer item «قضای نماز و روزه» (`qaza` tab in `MainActivity`). The bottom navigation is unchanged.
- A compact card on the daily checklist, directly under «فرائض روزانه», shows how many prayers are
  still owed (or an invitation to start) and opens the tracker.

## Prayers tab («نماز»)

- Counters for صبح، ظهر، عصر، مغرب، عشاء. Witr has its own switch («شمارش نماز وتر»), off by default;
  while it is off Witr is not counted in any total.
- Each prayer shows what is still owed, how many were made up, and a progress bar. «ادا شد» makes one
  up, «یکی فوت شد» adds one, and tapping the number edits it directly. A counter never goes below zero
  and is capped at 99,999 (still editable).
- Editing the remaining number keeps the progress already made (the total owed is recalculated).
- Summary card: a progress ring (made up ÷ owed), remaining and total, and a daily goal in «دست»
  (sets): one set is one prayer of every tracked kind. The estimated finish date is
  `ceil(largest remaining ÷ sets per day)` days from today, recalculated every time the screen opens, and
  is labelled as an estimate.
- «واگرد آخرین عمل» undoes the last action exactly. History is kept (last 200 actions) and survives
  restarts; the screen lists the latest eight with their dates.
- Empty state: one sentence explaining the feature, then «شروع (برآورد تعداد)» (setup helper) or
  «وارد کردن عدد» (manual counters).

### Setup helper (estimate)

A bottom sheet for people who do not know their number. It asks for a single thing: how many days
the user did not pray («تعداد روزهایی که نماز نخوانده‌اید»), plus whether to include Witr. The sheet
suggests one prayer per day for that many days and shows an editable number per prayer, so anything
else (days not required, a different count) is handled by editing the suggestions.

- It is labelled as an estimate, not a ruling.
- Nothing is saved until «ذخیره». Saving replaces the counters (undoable) and records a «ثبت برآورد
  اولیه» history entry.
- Hand-edited numbers are dropped when the days or the Witr choice change, so they never go stale.
  Inputs use `rememberSaveable`, so they survive rotation and language changes.

## Fasts tab («روزه»)

- Entries are blocks of missed fasts: a count, a free label (for example «رمضان ۱۴۰۴») and an optional
  reason. «افزودن روزه‌ی قضا» creates one; each entry can be edited or deleted (after confirmation).
- «یک روز ادا شد» records one made-up day with today's date; «واگرد» removes the most recent one. An
  entry's count cannot drop below the days already made up, and no more days than the count can be
  marked.
- The summary shows remaining fasts and overall progress; each entry shows its remaining count and the
  date of the last made-up day. Dates use the stored timestamp and are never recomputed.

## Streak

Making up a prayer or a fast counts as worship activity for the day (`markActivityToday()`), like
finishing a checklist task. Days already forgiven by the weekly streak freeze are unaffected.

## Storage

`QazaRepository` (`data/repository/`) keeps the whole state as one JSON document under the key `state`
in its own preferences file `qaza_tracker.xml`. The state is a pure Kotlin model (`qaza/QazaState.kt`)
whose changes return a new state, which keeps it easy to test. Parsing is tolerant: unknown fields,
unknown prayers, damaged entries and wrong types are skipped and the rest is kept; unreadable data gives
an empty tracker. The Room schema (version 1), permissions and reminder schedules are untouched.

The file is **not** excluded in `backup_rules.xml` / `data_extraction_rules.xml` (only `account.xml` and
`app_inbox.xml` are), so the counters and history are included in Android backup and device transfer.

## Code map

| Piece | File |
|---|---|
| Prayers, limits, estimate and finish-date helpers | `qaza/QazaCalculator.kt` |
| State, history, undo and fast entries | `qaza/QazaState.kt` |
| JSON storage | `data/repository/QazaRepository.kt` |
| ViewModel state and actions (`qaza…`) | `ui/viewmodel/AdhkarViewModel.kt` |
| Screen, setup sheet, dialogs, checklist card | `ui/screens/QazaScreen.kt` |
| Arabic strings | `ui/language/ArabicCatalog.kt` |

## Tests

`QazaCalculatorTest` (estimate, exempt days, caps, completion date), `QazaStateTest` (increments, never
below zero, progress kept on edit, undo including setup, history cap, fasts) and `QazaRepositoryTest`
(persistence across recreation and restart, undo after restart, round trip, tolerant parsing).

## Not included

Fidya/kaffara or any fatwa text, order-of-making-up rules, a Sunnah fasting calendar, a Ramadan
countdown, fasting or make-up reminders, widgets and cloud sync. Any future religious wording needs
primary sources and review first.
