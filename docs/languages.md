# App languages

General settings and onboarding offer فارسی / دری / العربية. Farsi remains the default, independently of the device language. The `app_language` preference persists the selection, and a StateFlow updates the interface immediately. Both interfaces remain RTL. A locale-specific context also localizes Android dialogs and resource strings without changing prayer calculations, saved method identifiers, timezone IDs, or reminder schedules.

The shared presentation components localize existing interface labels through the offline Arabic catalog. Formatted strings match complete templates with explicit placeholders; model IDs and stored values are not translated. Arabic mode uses Arabic digits and Arabic timezone labels. Article content has explicit Arabic variants with the same IDs. Custom entered content remains stored unchanged.

Persian translations are hidden in Arabic-mode adhkar cards, favorites, search previews, Quran cards, reminder bodies and shared adhkar. Original Arabic religious text is retained. Switching back to Persian restores the translations. Bibliographic source metadata is localized separately from religious text. Search in Arabic mode searches Arabic text only. Notifications and widgets use the persisted preference.

## Dari (Afghanistan)

Dari (`prs`, Android locale `fa-AF`) shares the Persian interface text, Persian digits, and the Persian adhkar meanings, Quran translations and tafsirs. `DariCatalog` changes only what differs in Afghan written use:

- **Solar months:** حمل، ثور، جوزا، سرطان، اسد، سنبله، میزان، عقرب، قوس، جدی، دلو، حوت. These are the same solar months, so dates and calculations do not change. تیر، مهر and دی are also ordinary words. They are replaced only as a whole label or beside a number (e.g. «۵ مهر ۱۴۰۵» becomes «۵ میزان ۱۴۰۵»), so «مهر» meaning kindness is left alone.
- **Gregorian months:** جنوری، فبروری، مارچ، اپریل، می، جون، جولای، اگست، سپتمبر، اکتوبر، نومبر، دسمبر. These are replaced only when the whole text is the month name, because «مه» is also a word.
- **Wording:** «گوشی» becomes «موبایل», including with suffixes such as «گوشی‌تان».

Replacements match whole words only and are idempotent, so text localized twice stays correct. Weekday and Hijri month names are the same in Dari. The regexes use brace-free `\pL` lookarounds for Android ICU. Text that bypasses `AppLanguage.text()` shows the shared Persian wording.

Tests: `AppLanguageTest` verifies formatted text, digit conversion, default fallback, and sharing without Persian translation. Verify changing language, opening collections, and reopening the app on a device before claiming installed behavior.

Android crash fix: escape both literal braces in the placeholder regexes. Android's ICU regex parser rejects the unescaped closing brace even though desktop JVM tests accepted it. The release was rebuilt and installed as a data-preserving update; switching from Persian to Arabic in Settings and a force-stop/relaunch with Arabic saved both succeeded on the connected Xiaomi 23129RAA4G. The Arabic home screen, including formatted countdown text, rendered successfully and the relaunched process had no crash-buffer entries. Release lint was skipped using the documented local-build flags.
