# Adhkar from the admin panel

New adhkar can be published from the admin panel («اذکار برنامه» in `/admin`) without an app update. They are shown next to the built-in adhkar in `AdhkarData`, which stay bundled and work offline.

## In the admin panel

A collection is one of two kinds:

- **Add to a built-in collection** (e.g. «اذکار صبحگاه»): its adhkar appear after that collection's own adhkar, and the collection's count on Home and «اذکار و ادعیه» includes them.
- **New collection**: a new tile on «اذکار و ادعیه», with a title, an optional short description and an icon from a fixed list the app can draw.

Each dhikr has the Arabic text, an optional Persian meaning, an optional source and a repeat count (1–10,000). Collections and adhkar can be reordered, hidden or deleted. Empty or hidden collections are not sent to the app.

## In the app

- **API:** `GET https://api.adhkar.ir/api/app-adhkar` returns the published collections in panel order. The ETag changes on any add, edit, reorder, toggle or delete.
- **No waiting on the network:** `AppAdhkarRepository.loadCached` applies the last saved response in `MainActivity.onCreate`, before any screen reads the collections.
- **Background refresh:** `AppAdhkarRepository.refresh` runs from `AppScaffold` at app start, at most once every 6 hours, with `If-None-Match`, so an unchanged list costs a 304. Failures keep the cached content and are retried on a later start.
- **Merging:** `AdhkarData.categories` and `AdhkarData.adhkarList` return the built-in content merged with the admin content (`mergeAdminCollections`). They are Compose state, so open screens update when a refresh brings changes. A key the app doesn't know (a collection from a newer version) is ignored.
- **Stable ids:** admin dhikr ids are the server id plus `1_000_000`, and new collections are `app_<server id>`, so saved progress, favorites and Home shortcuts keep working. Progress for a deleted dhikr stays in the database but is no longer shown.
- **Languages:** the Persian meaning is shown in Persian and Dari; Arabic, Urdu and Kurdish show the Arabic text, as for built-in adhkar. New collection titles are shown as written in the panel.

Tests: `AdminAdhkarTest` (merging and parsing) in the app; `tests/Feature/AppAdhkarTest.php` in the backend.
