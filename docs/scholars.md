# Scholars and lectures («علما و مشاهیر»)

Drawer destination `scholars`. Flow: scholar list → the scholar's lectures → full-screen player.

## Data source
Managed from the admin panel and served by the backend; the app never needs a release to add a sheikh or lecture.

- `GET https://api.adhkar.ir/api/scholars` → `{ "data": [ Scholar ] }`, scholars and lectures in display order, published only.
  - Scholar: `id`, `name`, `tagline`, `bio`, `hue` (0–360, tints the generated cover art), `lectures[]`.
  - Lecture: `id`, `title`, `description`, `audioUrl` (direct https mp3/m4a, ideally range-request capable so seeking works), optional `durationSec`.
- `ScholarsRepository`: shows the last cached response (or the bundled `assets/scholars.json` on first launch/offline), refreshes silently in the background; a failed refresh keeps what is shown.
- **Backend (nour-adhkar repo):** `scholars` and `lectures` tables serve the endpoint above; scholars and lectures are managed in the admin panel under «علما و سخنرانی‌ها» (add/edit/reorder/show-hide/delete; audio is an uploaded file up to 100 MB or an https link). Until the backend is deployed the app uses the bundled file.
- Bundled شیخ ضیایی and شیخ پردل have no lectures yet; the UI shows «به‌زودی».

## UI
- Cover art is drawn in Compose (gradient, star ornament, initial) and rotates slowly while playing. No image library or network images (a future `photoUrl` would need one).
- Player (`media/LecturePlayer.kt`, MediaPlayer streaming, one lecture at a time): play/pause, ±10 s, seek bar, speed 0.75–2×. Controls are LTR by convention. In-app playback only (no background service/notification yet).
