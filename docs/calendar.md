# Calendar (تقویم)

The drawer's «تقویم» destination shows one Jalali month at a time, with Hijri dates, a small set of
religious occasions, and the user's own activity. It is fully offline: no permissions, no network,
and no new stored data except the Hijri offset preference.

## Screen

From top to bottom:

1. **Month card**
   - The header shows the Jalali month and year, with the Hijri and Gregorian months it spans underneath.
   - Previous/next buttons and horizontal swipes change the month. «بازگشت به امروز» appears only when today is not already shown and selected.
   - The grid is Saturday-first and always six rows tall, so its height never jumps between months. Each cell shows the Jalali day and, below it, a small Hijri day number. Fridays are in the theme's error colour.
   - Markers: today has a solid accent background, and an occasion adds a small amber dot. There is no legend.
2. **Selected-day card.** This replaces a pop-up sheet, so tapping a day updates it immediately. It opens on today.
   - It shows the weekday with the full Jalali, Hijri and Gregorian dates, plus a relative chip («امروز», «فردا», «۳ روز دیگر», …).
   - Each occasion appears with its short description and source.
   - For today and past days it shows a progress row: whether activity was recorded, with the tasbih count and checklist items when known. Future days have no progress row.
3. **This month's occasions.** Tapping a row selects that day. Multi-day occasions (ایام تشریق) appear once, with a range.
4. **Hijri note.** It says that Hijri dates are calculated (Umm al-Qura) and may differ from local moon sighting. «تنظیم» opens a sheet where the user can shift Hijri dates by up to ±2 days, with a live preview of today's Hijri date.

The cells and rows use minimum heights rather than fixed heights, so text scales without clipping. Every cell has a merged TalkBack description: weekday, both dates, today/activity state, and occasions.

## Data and calculation

- **Jalali**: `calendar/Jalali.kt`, the published jalaali break-year algorithm, works on Julian Day Numbers. It is tested against the app's existing converter for every day from 2000 to 2040, and on leap years and Nowruz.
- **Hijri**: `calendar/Hijri.kt` uses Android's built-in ICU `IslamicCalendar` in Umm al-Qura mode (API 24+). It is tested to match the JDK's Umm al-Qura `HijrahChronology` for every day from 2020 to 2035. The offset (`hijri_offset`, −2…+2) shifts Hijri dates and Hijri-based occasions only. Jalali dates and Nowruz never move. The app makes no claim about which day is official in any region.
- **Activity**: the same sources as the streak: `isDayActive` over dhikr progress, tasbih sessions and the activity day keys. The tasbih count comes from recent tasbih sessions; checklist counts cover the last 30 days kept by preferences.

## Occasions

`calendar/Occasions.kt` is deliberately small. It holds only dates on which the major sources agree. Every religious entry cites its source:

| Occasion | Date | Source |
|---|---|---|
| آغاز سال هجری قمری | 1 Muharram | Qur'an 9:36 |
| تاسوعا | 9 Muharram | Sahih Muslim 1134 |
| عاشورا | 10 Muharram | Sahih Muslim 1162 |
| آغاز ماه رمضان | 1 Ramadan | Qur'an 2:185 |
| آغاز دهه آخر رمضان | 20 Ramadan (from sunset) | Sahih al-Bukhari 2017 |
| عید فطر | 1 Shawwal | Sahih al-Bukhari 1990; Sahih Muslim 1137 |
| آغاز دهه اول ذی‌الحجه | 1 Dhu al-Hijjah | Sahih al-Bukhari 969 |
| روز عرفه | 9 Dhu al-Hijjah | Sahih Muslim 1162 |
| عید قربان | 10 Dhu al-Hijjah | Sahih al-Bukhari 1990; Sahih Muslim 1137 |
| ایام تشریق | 11–13 Dhu al-Hijjah | Sahih Muslim 1141 |
| نوروز (calendar, not religious) | 1 Farvardin | — |

Disputed dates (for example Mawlid, Isra and Mi'raj, or 15 Sha'ban) are intentionally left out. Any addition needs a checkable source and review by someone knowledgeable before release.

## Out of scope

Device calendar sync, reminders for occasions, a home-screen widget, user-created events, and a Ramadan timetable view.

## Compact layout

The screen was tightened so the month and the selected day fit on one phone screen:

- **Header:** the month and year with one Hijri line. «امروز», shown only when today isn't on screen, and the previous/next arrows sit in the header row, so the grid never shifts down. The Gregorian dates are shown in the day card.
- **Grid:** only the rows a month needs (5 or 6), with height changes animated. Cells have a 46dp minimum height (about 48dp wide on a phone), with 2dp gaps.
- **Day card:** a 46dp day badge and two lines: the Jalali date, then «Hijri · Gregorian».
- **Occasions card:** shown only when the month has occasions.
- **Hijri note:** reduced to one line («تاریخ قمری: تقویم ام‌القری») with «تنظیم». The longer explanation is in the adjustment sheet.

