# Adhkar collection navigation

The Home section «اذکار و دعاها» has a «بیشتر» action that opens the separate اذکار و ادعیه (`adhkar`) collection page, which uses the existing category tiles and selection behavior. Morning, evening, sleep, daily, Quran prayers, and Sunnah prayers remain on Home and are excluded from this grid. Sleep and daily adhkar appear side by side in equal-width illustrated cards beneath morning and evening. Sleep uses a night illustration and daily adhkar uses a daytime landscape. All other categories and the existing counter shortcut move from Home to this page. Streak, prayer times, emotional verse, checklist and activity sections are unchanged. Opening a collection does not change the active tab, so leaving it returns to the collections page.

The app-sharing action is available in «درباره برنامه» rather than the navigation drawer.

The daily checklist home-screen widget shows today's tasks and progress. Tapping a task's row, status icon, or title toggles its completion for the current day, refreshes the list and progress, and uses the same saved checklist state as the in-app screen. The widget header and footer open the full checklist.

The achievements destination is opened from the Profile screen (drawer item «پروفایل»), not from the drawer, and its back button returns to Profile. It derives three awards from existing local data: active days, completed daily-checklist tasks, and saved tasbih counts. An emerald summary hero shows unlocked levels out of the total and the closest next goal; below it, scrollable filter chips and a warm-ivory two-column gallery (dark-mode aware) uses dedicated illustrated artwork, shield-shaped level badges, category filters, clear progress bars, and locked/unlocked milestones. Its centered app bar has a back button on the right and no information action. Tapping an award opens a full-screen visual detail page with a full-bleed hero, overlapping emblem, recent activity, current progress, and all three thresholds. The first visit establishes the current levels as a baseline; later level increases open a full-screen celebration with an explicit share action and Continue button. No account or network connection is required.

## First-run onboarding

New installations open a five-step, swipeable onboarding (the last step is an optional sign-in, see docs/account.md) before the main drawer UI. The flow introduces the primary offline features, lets the user select Persian or Arabic, offers daily-reminder and light/dark appearance choices, and ends with a localized summary. The stepper, back/continue controls, and horizontal gestures stay synchronized. Completing the final step persists the choice; notification permission is requested only when the user has kept daily reminders enabled. Prayer settings, location access, user content, and progress are not changed by onboarding.


The navigation drawer starts with a Telegram-style profile header (avatar, name/email, streak count) that opens Profile. It no longer lists Home, Quran, tasbih, daily checklist, Settings, achievements, Profile, messages, or favorites; those are reached from the bottom navigation, the header, Profile, and the home app bar (favorites heart beside the notifications icon).

## Verses for a feeling

Home's «امروز دلت چه حالی دارد؟» card has six feelings, each with 10 verses in `AdhkarData.emotionalAyat` (Arabic from the bundled Tanzil Uthmani text, Persian from Tanzil `fa.khorramdel`, the credited Khorramdel translation; long verses use the relevant clause). `PreferenceRepository.pickFeelingAyahId` keeps the same verse for a feeling all day, picks an unseen one on each new day, and starts a new round only after all 10 have been shown, so the same verse does not repeat for a feeling.
