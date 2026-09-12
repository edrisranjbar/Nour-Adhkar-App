# App languages

General settings include a language dropdown: فارسی / العربية. Farsi remains the default, independently of the device language. The `app_language` preference persists the selection, and a StateFlow updates the interface immediately. Both interfaces remain RTL. A locale-specific context also localizes Android dialogs and resource strings without changing prayer calculations, saved method identifiers, timezone IDs, or reminder schedules.

The shared presentation components localize existing interface labels through the offline Arabic catalog. Formatted strings match complete templates with explicit placeholders; model IDs and stored values are not translated. Arabic mode uses Arabic digits and Arabic timezone labels. Article content has explicit Arabic variants with the same IDs. Custom entered content remains stored unchanged.

Persian translations are hidden in Arabic-mode adhkar cards, favorites, search previews, Quran cards, reminder bodies and shared adhkar. Original Arabic religious text is retained. Switching back to Persian restores the translations. Bibliographic source metadata is localized separately from religious text. Search in Arabic mode searches Arabic text only. Notifications and widgets use the persisted preference.

Tests: `AppLanguageTest` verifies formatted text, digit conversion, default fallback, and sharing without Persian translation. Verify changing language, opening collections, and reopening the app on a device before claiming installed behavior.

Android crash fix: escape both literal braces in the placeholder regexes. Android's ICU regex parser rejects the unescaped closing brace even though desktop JVM tests accepted it. The release was rebuilt and installed as a data-preserving update; switching from Persian to Arabic in Settings and a force-stop/relaunch with Arabic saved both succeeded on the connected Xiaomi 23129RAA4G. The Arabic home screen, including formatted countdown text, rendered successfully and the relaunched process had no crash-buffer entries. Release lint was skipped using the documented local-build flags.
