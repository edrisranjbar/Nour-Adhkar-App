# Progress backup and sync

Google sign-in continues to use the existing Nour account API. Progress sync starts automatically whenever signed in, including existing accounts; there is no enable/disable setting. Profile shows status, manual retry and last successful sync time. Signed-out/offline use remains available.

The account backup includes streak activity days, daily checklist membership, saved tasbih/collection history, adhkar counters, favorites/custom dhikr, current tasbih counts, Quran last-read page/highlights/notes/khatm progress, and fasting progress. Credentials, installation identity, location, calculation/notification settings and audio files are excluded. Signing out stops sync and preserves local progress and the existing cloud backup. Old optional-sync preferences are ignored.

Changes are journalled before upload and retried roughly every 30 seconds while foregrounded, on resume, and when leaving the app. Execution while closed is not guaranteed. Individual activity/checklist/history records merge; repeated restores do not duplicate history. Newer writes win, with device UUID as the tie-breaker. Deletion tombstones prevent stale devices restoring removed items. Structured preference documents such as notes/khatm/fasting use the latest document in a conflict. Edits made during an upload are retained for the next upload. Backup includes all Room history, beyond the UI's 500-session window.

The authenticated version 1 `POST /api/progress/sync` endpoint and migration live in the companion `nour-adhkar` repository; see `PROGRESS_SYNC.md`. Requests are bounded to 4 MB/10,000 records. The Android database schema is unchanged. Android backup excludes the sync journal/device identity and downloaded audio.
