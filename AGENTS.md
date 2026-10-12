# Nour Adhkar — agent guidance

This file applies to the whole repository. Follow explicit user requests over these defaults. Verify the current code and device state before relying on historical build results.

## User workflow preferences

- Work and commit on `develop` by default. Never create a new branch unless the user explicitly asks for one.
- After committing, push `develop` and open a pull request from `develop` into `main`. Do not merge the pull request or push directly to `main` unless the user explicitly says so.
- Do not build, install, launch, or otherwise run the app unless the user explicitly asks you to do so. Source edits and non-executing checks are fine; defer build and runtime verification until requested.
- Ordinary requests to build, rebuild, install, or run on the phone mean the Bazaar **debug** variant for development. Build only `assembleBazaarDebug`; do not use generic `assemble` or build both store flavors. Use a release variant only when the user explicitly requests a release build, final release checks, store artifacts, or an update to the installed release package.
- Only release packaging requests require version-appropriate changelogs alongside the release artifacts in the same `.d` output directory. Bazaar gets AAB/BIN; Myket gets APK/AAB. A normal debug build does not require release packaging. Do not assume a build request also authorizes installing or launching the app.

## Release scope: refine, don't expand

This is a standing rule. Follow it unless the user strictly asks otherwise.

- Do not pack many new features into a single release. Keep each release small and focused.
- Prioritize enhancements, refinement of existing features, a much better UI and UX, and bug fixes over adding new functionality.
- Before proposing or adding a new feature, consider whether improving, polishing, or fixing something that already exists would serve the user better. Prefer that.
- If a request would add several features at once, do the most valuable one (or the refinement behind it) and flag the rest as follow-ups instead of bundling them.
- Only set this rule aside when the user explicitly and strictly requests more features or a larger release.

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
- The bottom navigation contains exactly these destinations in order: Home, Quran, tasbih (matching the centered circular control), daily checklist, and Settings. Keep the broader destinations and actions in the navigation drawer; app sharing belongs in the About screen, not the drawer. Among home collection cards, retain morning, evening, Quran prayers, and Sunnah prayers. Other adhkar collections belong on the separate `adhkar` page titled «اذکار و ادعیه». Preserve existing streak/activity/checklist sections unless asked to change them.
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
.\gradlew.bat assembleBazaarDebug -x lint --project-cache-dir .gradle-card-design --no-configuration-cache
.\gradlew.bat testBazaarDebugUnitTest --tests com.example.PrayerSettingsTest --project-cache-dir .gradle-card-design --no-configuration-cache
```

- For an explicitly requested release update, build `assembleBazaarRelease` or `assembleMyketRelease` with the same environment and cache options. Signing is already configured through local ignored credentials; never print those credentials. See `docs/store-releases.md` and `scripts/Release-Stores.ps1` for both-store packaging and independent publication. Bazaar gets AAB/BIN; Myket gets APK/AAB, each with its changelogs.
- During debug iteration, use `-x lint` and avoid invoking lint/test/release tasks unless requested or required by the change. Debug assembly normally does not invoke lint. Use `--offline` when the required dependencies are cached and no dependency changes are needed; retry online if an artifact is missing. Do not claim skipped checks passed. For Compose UI tweaks, prefer existing `@Preview` support in Android Studio when it can answer the layout question without installation.
- Gradle performance settings: pin `org.gradle.daemon=true` and `org.gradle.vfs.watch=true` when applying performance tuning. Before increasing `org.gradle.jvmargs` from `-Xmx4g` to `-Xmx6g` and adding `-XX:+UseParallelGC`, verify that available RAM can support Gradle, Kotlin, Android Studio, and other active work without paging.
- Re-test configuration-cache compatibility with the current AGP before enabling `org.gradle.configuration-cache=true` or removing `--no-configuration-cache`; the navigation resource task previously blocked it. Keep the workaround until a requested verification confirms compatibility.
- Prefer the Kotlin compiler daemon for incremental builds once the previous daemon-connection failure is resolved. Remove `kotlin.compiler.execution.strategy=in-process` only after a requested build confirms daemon operation; do not remove the fallback blindly or start a build merely to tune settings.
- The project-local `.gradle-user-home` previously failed with `Unexpected lock protocol ... Expected 3, found 0`. Using the standard user cache resolved it. Do not delete shared caches or terminate unrelated builds without investigating.
- Pass the AAPT2 override through the environment as above; an unquoted dotted `-P` argument was misparsed by PowerShell.
- Release lint previously failed resolving desktop-only Compose dependencies. Investigate rather than globally disabling lint. For an explicitly requested local release phone build, a temporary command-only fallback used `-x lintVitalAnalyzeRelease -x lintVitalReportRelease -x lintVitalRelease`; disclose that lint was skipped and never report it as passing. Do not skip release validation for final checks or store uploads by default.
- KSP has emitted an AWT exception even on successful builds. Determine success from the final Gradle result and exit code, not a single log line.
- Run focused tests for changed behavior and build the actual variant being delivered. Visually check material UI changes on-device when available. Never claim a test or interaction was verified unless it was actually run.

## Installing on the user's phone

- For development, install the Bazaar debug APK as `ir.adhkar.app.debug`, alongside the real app (`ir.adhkar.app`). It has separate app data and does not update or replace the release installation. A request to install or run normally means debug, even when the user calls it an update. Use release only when explicitly requested or when the user specifically requires replacing the installed `ir.adhkar.app` package.
- Keep the permanent release signing key. Never expose, regenerate, rotate, or commit signing credentials/keystores. Verify package and signing compatibility when needed; Android must accept the update normally.
- Discover the connected device with `adb devices -l`; do not assume a previously used serial is still the intended device.
- Default development APK: `app/build/outputs/apk/bazaar/debug/app-bazaar-debug.apk`. Build it with `assembleBazaarDebug` only. `scripts/Run-Phone.ps1` currently builds and installs release; do not use that script for a default debug request unless it is first updated to support debug explicitly.
- Explicit release APK: `app/build/outputs/apk/bazaar/release/app-bazaar-release.apk` (or `myket/release/app-myket-release.apk` for an explicitly requested Myket release). Both release flavors retain `ir.adhkar.app` and the same signing key.
- Install with `adb -s <serial> install -r <apk>`, preserving data. Never uninstall, clear app data, or force a downgrade to bypass an installation problem without explicit approval.
- For a requested development run, launch `ir.adhkar.app.debug/com.example.MainActivity`; for an explicitly requested release run, launch `ir.adhkar.app/com.example.MainActivity`. Verify the selected package's installed version/update time and foreground activity, and inspect the affected screen. A successful build alone does not complete a phone-deployment request.
- If Android blocks installation, report the exact error and ask the user to approve the phone prompt. Do not bypass device security settings silently.
- Do not bump version values, publish to Cafe Bazaar, or submit store artifacts unless requested. Do not claim store acceptance or Play Protect clearance without evidence.
- Use Android Studio's Apply Changes for supported code/resource edits during a debug development session when available. Verify that the session actually applies the change; do not promise it for every edit. Live Edit is not configured, and the release is not debuggable. Do not promise Flutter-style hot reload or change release signing/package/debuggability.
