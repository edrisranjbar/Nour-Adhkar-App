# App languages

General settings and onboarding offer فارسی / دری / اردو / العربية. Farsi remains the default, independently of the device language. The `app_language` preference persists the selection, and a StateFlow updates the interface immediately. Both interfaces remain RTL. A locale-specific context also localizes Android dialogs and resource strings without changing prayer calculations, saved method identifiers, timezone IDs, or reminder schedules.

The shared presentation components localize existing interface labels through the offline Arabic catalog. Formatted strings match complete templates with explicit placeholders; model IDs and stored values are not translated. Arabic mode uses Arabic digits and Arabic timezone labels. Article content has explicit Arabic variants with the same IDs. Custom entered content remains stored unchanged.

Persian translations are hidden in Arabic-mode adhkar cards, favorites, search previews, Quran cards, reminder bodies and shared adhkar. Original Arabic religious text is retained. Switching back to Persian restores the translations. Bibliographic source metadata is localized separately from religious text. Search in Arabic mode searches Arabic text only. Notifications and widgets use the persisted preference.

## Dari (Afghanistan)

Dari (`prs`, Android locale `fa-AF`) shares the Persian interface text, Persian digits, and the Persian adhkar meanings, Quran translations and tafsirs. `DariCatalog` changes only what differs in Afghan written use:

- **Solar months:** حمل، ثور، جوزا، سرطان، اسد، سنبله، میزان، عقرب، قوس، جدی، دلو، حوت. These are the same solar months, so dates and calculations do not change. تیر، مهر and دی are also ordinary words. They are replaced only as a whole label or beside a number (e.g. «۵ مهر ۱۴۰۵» becomes «۵ میزان ۱۴۰۵»), so «مهر» meaning kindness is left alone.
- **Gregorian months:** جنوری، فبروری، مارچ، اپریل، می، جون، جولای، اگست، سپتمبر، اکتوبر، نومبر، دسمبر. These are replaced only when the whole text is the month name, because «مه» is also a word.
- **Wording:** «گوشی» becomes «موبایل», including with suffixes such as «گوشی‌تان».

Replacements match whole words only and are idempotent, so text localized twice stays correct. Weekday and Hijri month names are the same in Dari. The regexes use explicit Arabic-script/Latin letter ranges instead of `\pL`. Android ICU rejected `\pL`, so choosing Dari crashed on the device, even though desktop JVM tests passed. A test now blocks Unicode property syntax in `DariCatalog`. Text that bypasses `AppLanguage.text()` shows the shared Persian wording.

## Urdu (Pakistan)

Urdu (`ur`, Android locale `ur-PK`) is a separate language, not a Persian variant. It has its own complete interface catalog, `UrduCatalog`. The catalog is keyed by the same Persian source text as `ArabicCatalog`, and both run on the shared `PhraseCatalog` engine (literal phrases plus `{0}`/`{1}` templates, which Urdu reorders where its word order needs). `UrduCatalogTest` fails if any Arabic catalog key lacks an Urdu entry, so new interface text needs an Urdu line. Urdu uses the extended Arabic-Indic digits (U+06F0…) that it shares with Persian. The UI font (Vazirmatn) covers every Urdu letter (ٹ ڈ ڑ ں ھ ہ ے ۓ). It is a Naskh face; a Nastaliq font is a possible follow-up.

- **Hard-coded labels:** labels that used to be written as `if (arabic) "…" else "…"` (Quran reader, khatm plan, spotlight search, sync card, prayer widget) now use `pick(arabic, persian)`. `pick` localizes the Persian source through `AppLanguage.text()`, so Urdu (and Dari) reach them. Persian is unchanged.
- **Adhkar meanings:** adhkar show the original Arabic without the Persian meaning, as in Arabic mode. No Urdu adhkar meanings are bundled yet, and religious text is not machine-translated. Source citations use an Urdu spelling table (سورہ، آیت، بقرہ …).
- **Quran:** the Urdu translation is Muhammad Ibrahim Junagarhi's, from QuranEnc (`ur_junagarhi`; see `docs/quran.md`). Tafsir options are the Arabic tafsirs, because no Urdu tafsir is bundled.
- **Dates:** the khatm deadline, the sync time and the tasbih history use the device's Gregorian format for Urdu, as for Arabic. The calendar screen still shows the Jalali calendar first.
- **Not yet in Urdu:** articles (they have explicit Arabic versions only, so Urdu shows the Persian article) and text that bypasses `AppLanguage.text()`.

Tests: `AppLanguageTest` verifies formatted text, digit conversion, default fallback, and sharing without Persian translation. Verify changing language, opening collections, and reopening the app on a device before claiming installed behavior.

Android crash fix: escape both literal braces in the placeholder regexes. Android's ICU regex parser rejects the unescaped closing brace even though desktop JVM tests accepted it. The release was rebuilt and installed as a data-preserving update; switching from Persian to Arabic in Settings and a force-stop/relaunch with Arabic saved both succeeded on the connected Xiaomi 23129RAA4G. The Arabic home screen, including formatted countdown text, rendered successfully and the relaunched process had no crash-buffer entries. Release lint was skipped using the documented local-build flags.
