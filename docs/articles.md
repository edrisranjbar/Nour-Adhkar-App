# Articles («مقالات»)

Articles come from the API, not from the app bundle.

- Source: `GET https://api.adhkar.ir/api/posts` (published posts, newest first). Each post maps to an article: `slug` → id, `title`, `excerpt` → summary (falls back to the first 140 characters), `content`, `user.name` → author (default «اذکار نور»); reading time is estimated from word count. HTML from the admin editor is converted to plain text.
- Offline: the last successful response is cached in the `articles` shared preferences and shown immediately; the screen refreshes in the background (thin progress bar). With no cache, a centered spinner shows while loading, then either the list, «هنوز مقاله‌ای منتشر نشده است.», or a friendly Persian error with «تلاش دوباره».
- Arabic: translations for the seeded articles are keyed by slug in `ui/language/ArabicArticles.kt`; other posts show as published.
- Content: the backend `AppArticlesSeeder` publishes two sourced articles (`virtue-of-remembering-allah`, `sayyid-al-istighfar`). Run `php artisan db:seed --class=AppArticlesSeeder` on the server (idempotent; needs an admin user). New articles are added from the admin panel as published posts.
