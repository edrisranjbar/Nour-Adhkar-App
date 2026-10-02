# Articles («مقالات»)

Articles are written and managed in the admin panel (`/admin/articles`: list, write, edit, publish or unpublish, delete). They come from the API, not from the app bundle.

- **Source:** `GET https://api.adhkar.ir/api/app-articles` returns every published article, newest first, in one response with an `ETag`. Each item maps to an article:
  - `slug` becomes the article id;
  - `title` and `content` are used as is;
  - `excerpt` becomes the summary, falling back to the first 140 characters of the content.
  - The author shows as «اذکار نور». Reading time is estimated from the word count. Older HTML content is converted to plain text.
- **Speed:**
  - The app warms the cache in the background at launch.
  - The last response is kept on disk (`articles` shared preferences) and parsed in memory, so the list appears instantly and nothing is re-parsed.
  - Opening the screen refreshes quietly, with no progress bar over cached articles.
  - Refreshes are skipped for 10 minutes after a check, and otherwise send `If-None-Match`, so an unchanged list returns a small 304.
  - «تلاش دوباره» forces a refresh.
  - The cache from the older `/api/posts` endpoint is still read until the first new response replaces it.
- **No cache yet:** a centred spinner shows while loading. Then either the list appears, or «هنوز مقاله‌ای منتشر نشده است.», or a friendly Persian error with «تلاش دوباره».
- **Arabic:** translations for the seeded articles are keyed by slug in `ui/language/ArabicArticles.kt`. Other articles show as published.
- **Seeded content:** the backend `AppArticlesSeeder` publishes two sourced articles (`virtue-of-remembering-allah`, `sayyid-al-istighfar`).
