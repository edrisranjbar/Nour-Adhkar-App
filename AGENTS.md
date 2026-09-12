# Nour Adhkar — agent guidance

This file applies to the whole repository. Follow explicit user requests over these defaults. Verify the current code and device state before relying on historical build results.

## Product and architecture

- Persian-first, RTL Android app for adhkar, prayers, reading, reminders, and personal progress. Keep core content and calculations offline.
- Kotlin, Jetpack Compose, Material 3, StateFlow/Coroutines, Room, and preferences. Reuse existing components and repository/ViewModel boundaries.
- Main app navigation and drawer: `app/src/main/java/com/example/MainActivity.kt`.
- Screens: `app/src/main/java/com/example/ui/screens/`; shared theme: `ui/theme/`; state/actions: `ui/viewmodel/AdhkarViewModel.kt`.
- Prayer calculations: `app/src/main/java/com/example/prayer/PrayerSettings.kt`. Refer to `docs/prayer-times.md` and `docs/adhkar-navigation.md` for behavior.
- Inspect the worktree before editing. Preserve unrelated changes, existing data, and user settings. Do not reset, clean, or overwrite the worktree to simplify a task.

## UI and navigation conventions

- Use natural Persian labels, Persian digits where appropriate, RTL layout, accessible contrast, scalable text, and adequate touch targets. Avoid fixed heights that clip text at larger font scales.
- Use shared Material theme roles for dark surfaces, text, dividers, and highlights. Do not introduce a separate blue/mint dark palette for the prayer card.
- Keep the prayer-times card compact, immediately after the streak section. It shows the next prayer/countdown and six times in three two-column rows, with a gear settings icon. Do not show a date or restore the large illustration. Keep the date internally for correct calculation rollover.
- Among home collection cards, retain morning, evening, Quran prayers, and Sunnah prayers. Other adhkar collections belong on the separate `adhkar` page titled «اذکار و ادعیه», the second drawer item after Home. Preserve existing streak/activity/checklist sections unless asked to change them.
- Article cards open a separate reading view, not an expanding card. Provide back navigation and an Android share chooser for the article. Do not repeat «مقالات» or a page description beneath the app bar. Opening the chooser is not authorization to send to a recipient.
- Preserve the selected collection's parent page when returning. Avoid duplicate app-bar and body titles.
- Update relevant documentation when behavior changes. Report source changes, compilation, installation, and visual verification separately.

## Prayer and reminder correctness

- Preserve saved method identifiers and the default unless explicitly asked to change them. Calculation method and Asr madhab are independent settings.
- Use the library's native presets; do not invent times, religious claims, or official regional endorsements. Research primary sources before adding/changing calculation parameters.
- Current options: MWL, Karachi, Egyptian, Kuwait, Umm al-Qura, North America/ISNA, Qatar, Dubai (Adhan model), Singapore, and Moonsighting Committee.
- Umm al-Qura currently uses a 90-minute Isha interval. Its additional Ramadan adjustment is not automatic; preserve the explicit settings warning until a tested implementation replaces it.
- Use the saved location's timezone for timetable dates, including tomorrow's Fajr. Exclude sunrise from next-prayer selection. Show unavailable times honestly; do not silently substitute sample times or locations.
- Keep foreground location permission explicit. Do not add background location or change reminder schedules as a side effect of UI work.
- Reminder snooze is one hour, not ten minutes. Preserve existing day-selection and scheduling semantics.
- Relevant focused tests: `app/src/test/java/com/example/PrayerSettingsTest.kt`.

## Build and verification on this Windows workstation

Use the checked-in Gradle wrapper. Resolve installed paths before running; these are known working local defaults, not portable requirements:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:GRADLE_USER_HOME = 'C:\Users\edi\.gradle'
${env:ORG_GRADLE_PROJECT_android.aapt2FromMavenOverride} = 'C:\Users\edi\AppData\Local\Android\Sdk\build-tools\36.0.0\aapt2.exe'
.\gradlew.bat assembleDebug --project-cache-dir .gradle-card-design --no-configuration-cache
.\gradlew.bat testDebugUnitTest --tests com.example.PrayerSettingsTest --project-cache-dir .gradle-card-design --no-configuration-cache
```

- For a release update, build `assembleRelease` with the same environment and cache options. Signing is already configured through local ignored credentials; never print those credentials.
- The project-local `.gradle-user-home` previously failed with `Unexpected lock protocol ... Expected 3, found 0`. Using the standard user cache resolved it. Do not delete shared caches or terminate unrelated builds without investigating.
- Pass the AAPT2 override through the environment as above; an unquoted dotted `-P` argument was misparsed by PowerShell.
- Release lint previously failed resolving desktop-only Compose dependencies. Investigate rather than globally disabling lint. For an explicitly requested local phone build, a temporary command-only fallback used `-x lintVitalAnalyzeRelease -x lintVitalReportRelease -x lintVitalRelease`; disclose that lint was skipped and never report it as passing.
- KSP has emitted an AWT exception even on successful builds. Determine success from the final Gradle result and exit code, not a single log line.
- Run focused tests for changed behavior and build the actual variant being delivered. Visually check material UI changes on-device when available. Never claim a test or interaction was verified unless it was actually run.

## Installing on the user's phone

- The user wants updates to the existing «اذکار نور» app, not a second app. Release package: `ir.adhkar.app`; debug package: `ir.adhkar.app.debug`. Do not install the debug variant when asked to update the existing app.
- Keep the permanent release signing key. Never expose, regenerate, rotate, or commit signing credentials/keystores. Verify package and signing compatibility when needed; Android must accept the update normally.
- Discover the connected device with `adb devices -l`; do not assume a previously used serial is still the intended device.
- Release APK: `app/build/outputs/apk/release/app-release.apk`.
- Install with `adb -s <serial> install -r <apk>`, preserving data. Never uninstall, clear app data, or force a downgrade to bypass an installation problem without explicit approval.
- Launch `ir.adhkar.app/com.example.MainActivity`, verify installation/update time and foreground activity, and inspect the affected screen. A successful build alone does not complete a phone-deployment request.
- If Android blocks installation, report the exact error and ask the user to approve the phone prompt. Do not bypass device security settings silently.
- Do not bump version values, publish to Cafe Bazaar, or submit store artifacts unless requested. Do not claim store acceptance or Play Protect clearance without evidence.
- Live Edit is not configured. The installed release is not debuggable; do not promise Flutter-style hot reload or change signing/package/debuggability without an explicit setup request.
