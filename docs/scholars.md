# Scholars and lectures («علما و مشاهیر»)

Drawer destination `scholars`. Flow: scholar list → the scholar's lectures → full-screen player.

- Catalog: `app/src/main/assets/scholars.json` (offline, bundled). Loaded by `ScholarsRepository`.
  - Scholar: `id`, `name`, `tagline`, `bio`, `hue` (0–360, tints the generated cover art), `lectures[]`.
  - Lecture: `id`, `title`, `description`, `audioUrl` (direct https mp3), optional `durationSec`.
- Current scholars: شیخ ضیایی, شیخ پردل. Their `lectures` are empty until verified audio sources and biographies are supplied; the UI shows «به‌زودی» meanwhile.
- Cover art is drawn in Compose (gradient, star ornament, initial); it rotates slowly while playing. No image library or network images.
- Player (`media/LecturePlayer.kt`, MediaPlayer streaming, one lecture at a time): play/pause, ±10 s, seek bar, speed 0.75–2×. Controls are LTR by convention. Playback is in-app only (no background service/notification yet).
