# Prayer times and settings

## Home screen widget

The launcher offers a separate 2×2 «اوقات شرعی» widget. It uses the same saved location, timezone, calculation method, and Asr setting as the in-app card. Its compact three-row timetable shows Fajr, sunrise, Dhuhr, Asr, Maghrib, and Isha; the next-prayer line excludes sunrise and rolls over to tomorrow's Fajr after Isha. Times are formatted in the saved location's timezone. Missing or invalid location shows a setup prompt rather than sample times. Tapping the widget opens prayer settings. Existing launcher placements retain their previous grid size until the user resizes or re-adds the widget.

The widget follows the selected app language and light/dark preference. It refreshes when prayer settings, language, or theme change; on app resume, boot, app update, device time or timezone changes; on the launcher's periodic update; and around the next prayer or saved-location midnight through an inexact, non-wakeup alarm. This alarm updates the display only and is independent of adhan playback and reminder scheduling.

Prayer settings are grouped into four outlined cards: adhan voice, enabled prayer alerts, location/timezone, and calculation method/Asr. Cards have 16 dp internal padding and control spacing, separated by 24 dp. Voice and prayer-alert choices still save immediately; location and calculation drafts use the full-width Save button. Asr uses a read-only dropdown with an arrow, independently of the calculation-method dropdown. Automatic and manual location modes share the same timezone group.

Manual location now uses city-name search through Android Geocoder, without coordinate input fields or a location permission request. Search runs off the main thread and may require internet. Users choose a matching city/region/country result; its coordinates become a draft. Editing the city invalidates the previous selection and disables Save until a result is selected. Existing saved locations remain valid. The timezone stays a separate explicit selection and is not guessed from a city name. A failed search does not silently substitute coordinates.

Dark-mode prayer card colors now come from the shared Material theme: surface, onSurface, onSurfaceVariant, outlineVariant, tertiary accents, and secondaryContainer highlights. The compact layout and light-mode palette are unchanged.

The method dropdown additionally offers Umm al-Qura, North America (ISNA), Qatar, Dubai (Adhan model), Singapore, and Moonsighting Committee using the library's native enum parameters. Existing identifiers/defaults and the independent Asr setting are preserved. Umm al-Qura currently uses the library's 90-minute Isha interval: the settings UI explicitly warns that Ramadan's additional 30 minutes are not automatically applied. All ten available methods are covered by chronological timetable tests for both Asr options.

Settings has three Persian tabs: General, Notifications, and Prayer times. Existing reminder switches and schedules remain in Notifications.

The home screen displays six times (Fajr, sunrise, Dhuhr, Asr, Maghrib, Isha) after the user saves a location. No location is silently assumed. Settings offers a persisted choice between automatic GPS/network location detection and manual city search. Automatic mode checks the system location-services state before requesting a fix. If location services are off, it opens Android's location settings; after the user enables them and returns, detection continues automatically. Android does not permit the app to enable GPS silently. Saving automatic mode requires a detected location. Existing manually saved locations retain manual mode. Detection requests foreground location permission, accepts approximate location where available, and stops all requests after a result, a 25-second timeout, or leaving the screen. Detected coordinates remain a draft until saved; the time zone is taken from the device and explicitly presented for review. No background location is requested. The method and time zone are read-only dropdown fields with arrows. The time zone dropdown stores device-supported IDs but displays Persian ICU location names and current UTC offsets, with the selected zone, device zone, Tehran, and UTC first. Location and calculation preferences persist across restarts; travelers must update their saved location.

Calculations run offline using [Adhan Java 1.2.1](https://github.com/batoulapps/adhan-java). Supported presets are Muslim World League, Karachi, Egyptian, and Kuwait, with standard or Hanafi Asr. Dates and displayed times use the selected location's time zone, independently of the device zone. The redesigned card refreshes every 15 seconds, shows a Persian date, six icon tiles, and a next-prayer countdown. Sunrise is excluded from next-prayer selection. After Isha the banner uses Fajr on the next local calendar day; the tiles retain the current day. The settings icon and empty-state button open prayer settings directly. Unavailable astronomical times show a dash rather than an invented time.

These settings control the displayed prayer timetable. The existing Adhkar reminder schedules remain user-selected clock times.

The home card now uses matching ivory/emerald and charcoal/mint palettes following the app's selected theme. A decorative vector sunrise separates the location/settings header from the outlined next-prayer panel. Six stacked timetable rows replace the icon tiles, highlighting only the next prayer in today's schedule. Real calculated times, the minute-based countdown, date, location setup, and unavailable-time handling are unchanged; no sample times or additional religious calculation entries were introduced.

The compact revision places the prayer card immediately after the streak section. It removes the large decorative horizon, reduces spacing and banner typography, and arranges all six times in three two-column rows to target roughly half the previous height without fixing or clipping the card height.

Users may add a manual correction of up to ±30 minutes to each of the six times (`PrayerSettings.offsets`, stored as `prayer_offsets`, default all zero). It is applied in `times()`, so the card, widget and adhan/reminder scheduling all agree; method and Asr settings are unchanged.

The correction section is titled «اصلاح زمان‌های محاسبه‌شده». Each time has labeled one-minute earlier/later controls, a plain-language correction status, and today's calculated/corrected preview using the draft location, method, madhab and saved/draft timezone. The preview rolls over with the location's date and is unavailable until a valid location is selected; unavailable calculated times are shown honestly. Individual and all-time reset actions clear only the draft corrections. Corrections remain limited to ±30 minutes and take effect only after Save; the section explains their effect on displayed times, adhan and reminders.

Unit coverage includes invalid settings, chronological ordering, calculation method changes, Hanafi Asr, and location-date selection across UTC midnight.

## Adhan sound selection

Settings uses one compact `موذن` dropdown for six bundled choices: Ali ibn Ahmad Mulla from Makkah, the Adhan of Al-Masjid an-Nabawi in Madinah, Mishary Rashid Alafasy, Abdulbasit Abdusamad, Nasser Al Qatami, and Yasser Al-Dosari. Choosing an item saves it immediately as a stable recording ID, independently of location settings. Existing choices and saved IDs are preserved. A single adjacent play/stop icon previews the selected sound. No file picker, storage permission, audio download, source list, or credits dialog appears in the UI. Preview stops on completion, playback errors, audio-focus loss, app backgrounding, or leaving the section. Source details remain in `licenses/adhan-audio.md`.

Five independent checkboxes (Fajr, Dhuhr, Asr, Maghrib, Isha) enable automatic Adhan playback. All are off by default and selections save immediately. Sunrise is excluded. Playback requires a saved valid location, a selected voice, notification permission, and exact-alarm access on Android 12+. Missing prerequisites appear beside the controls. Existing Adhkar reminders remain independent.

Each enabled prayer has a separate exact idle-aware alarm for its next calculated occurrence using the saved timezone. Changing location, voice, or enabled prayers refreshes the schedule; disabling a prayer cancels its alarm. Schedules also refresh on app resume, reboot, app update, clock changes, timezone changes, and exact-alarm permission grants. Unavailable times are skipped. The receiver rechecks enabled state and ignores alerts delayed by more than ten minutes. Audio runs in a media-playback foreground service using alarm volume and a visible Stop notification; completion, errors, or audio-focus loss stop playback. Boot receivers only schedule future alarms and do not start audio. Invalid/legacy file selections resolve to no built-in selection until a user chooses one.

Focused test coverage includes selection persistence, invalid IDs, unchanged reminder time, six distinct resources, and hashes matching the downloaded source audio. The six files were fully decoded with FFmpeg to verify readability; updated Android tests and compilation are deferred until a build is requested.

## Battery optimization (background adhan and reminders)

Adhan alarms and adhkar reminders must fire while the app is closed and the phone is idle, so the app asks to be exempt from battery optimization (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, helper `notifications/BatteryOptimization.kt`).

- The system dialog opens only from a user action: when the first adhan prayer is ticked, or when the adhkar reminder switch is turned on. Declining changes nothing else; schedules and settings stay as they are.
- Until the exemption is granted, `BatteryOptimizationNotice` appears under the adhan checkboxes and under the reminder switch, with the button «اجازه اجرا بدون محدودیت باتری». It re-checks on every resume and hides once granted.
- If the direct dialog is unavailable, the battery-optimization list and then the app's details page open instead.
- Some vendors (e.g. Xiaomi autostart) have extra background limits outside Android's API; those must still be enabled by the user in the phone's settings.

### Reminder health card

Under the reminder switch (Settings → reminders), `ReminderHealthCard` replaces the plain battery notice:

- Two read-only checks refresh on every resume: notifications allowed (app-level and the `nour_adhkar_reminders` channel, `ReminderHealth.notificationsAllowed`) and battery exemption. A failing check shows a button that opens the matching system screen.
- «ارسال اعلان آزمایشی» schedules a notification about 10 seconds later with the same `setAndAllowWhileIdle` call as real reminders (`AdhkarNotificationManager.scheduleTestReminder`), so the user can lock the phone and see whether reminders survive battery management. It never changes saved reminder times, days, snooze or other schedules.
- After a test, «اگر اعلان نرسید» shows short steps for the phone's brand (`ReminderVendorGuide`, by `Build.MANUFACTURER`: Xiaomi/Redmi/POCO, Huawei/Honor, Oppo/Realme/OnePlus, Vivo/iQOO, Samsung, generic otherwise) and a button to the app's settings page. The text describes what to enable and notes that menu names vary by model; no vendor-specific settings screens are launched.
- Morning/evening reminders still use inexact `setAndAllowWhileIdle`; only adhan alarms are exact. No permissions were added.

## Adhkar after prayer reminder

Settings → prayer times → «یادآوری اذکار پس از نماز» has one global switch for all five prayers. Each notification «اذکار پس از نماز …» opens the `after_salah` collection («اذکار پس از نماز»). An existing per-prayer selection keeps the feature enabled for all prayers after updating; an empty selection remains disabled.

- **Timing:** 10 minutes after the configured prayer start: prayer starts 30 minutes after adhan, except Maghrib starts 5 minutes after adhan. Thus reminders fire 40 minutes after adhan for Fajr, Dhuhr, Asr and Isha, and 15 minutes after adhan for Maghrib (`PostPrayerReminders`). These offsets implement the requested app behavior, not an inferred local congregation timetable. Calculation follows the saved location, method, Asr madhab and time zone, exactly like the adhan alarms.
- **Opt-in:** nothing is scheduled until the global switch is enabled. Enabling offers the battery exemption, like adhan. The reminder does not need the adhan sound, the adhan checkboxes or the exact-alarm permission.
- **Scheduling:** one inexact `setAndAllowWhileIdle` alarm per prayer (`PostPrayerReminderScheduler`), refreshed from the same triggers as adhan alarms (boot, app update, time or zone change, app start, settings changes, every adhan alarm). Disabling cancels all five alarms. An alarm delivered more than 30 minutes late, or while globally disabled, is dropped; the next day's alarm is scheduled when enabled.
- **Needs:** a saved valid location and allowed notifications; the settings show what is missing. Channel: `post_prayer_reminders`, which the user can mute separately from adhkar reminders.
- The existing morning/evening reminders, their days and the one-hour snooze are unchanged.

## Saved places

Users can keep up to 10 named places (for example «خانه», «مشهد», «سفر») and switch between them. The active place drives everything that uses a location: the home prayer card, adhan alarms, post-prayer reminders, the widget and Qibla.

- **What belongs to a place:** city, coordinates, timezone, calculation method, Asr madhab and minute corrections. The muezzin voice, the prayers that play the adhan, and reminder settings stay global.
- **Switching:** the prayer card shows the active place as a chip. Tapping it opens «مکان اوقات شرعی»: radio rows with name, city, UTC offset and the next prayer there. One tap switches, the sheet closes, and a short message confirms. Alarms are rescheduled immediately.
- **Managing (Settings → اوقات شرعی → «مکان‌ها»):**
  - Tap a place to make it active.
  - The ⋮ menu offers activate, rename, move up or down, and delete. Delete asks first. The last place can't be deleted, and deleting the active place activates the first remaining one.
  - The editor cards below always edit the active place. Their titles name it, for example «روش محاسبه · مشهد».
- **Adding:** «افزودن مکان» opens the same editor for a fresh draft. It starts with a name field and quick names. Location comes from city search or GPS detection; GPS is only a way to add a place, not a live entry. The timezone is still an explicit choice. The method, madhab and corrections are copied from the active place. Saving adds the place and makes it active. Leaving Settings abandons the draft.
- **Place name shown:** the widget header and the adhan notification title show the active place's name, for example «اذان ظهر · خانه». Before any place is saved, the widget keeps its setup prompt.
- **Storage:** `prayer_places` (JSON) and `prayer_active_place`. The original `prayer_*` keys always mirror the active place, so the widget and schedulers read prayer settings unchanged.
- **Migration:** on first launch after the update, an existing saved location becomes place #1, named after its city, with every value preserved. Places are not part of progress sync.
