package com.example.data.model

data class DailyChecklistEntry(val id: String, val title: String)

object DailyChecklistData {
    val items = listOf(
        DailyChecklistEntry("salah_fajr", "نماز صبح"),
        DailyChecklistEntry("salah_dhuhr", "نماز ظهر"),
        DailyChecklistEntry("salah_asr", "نماز عصر"),
        DailyChecklistEntry("salah_maghrib", "نماز مغرب"),
        DailyChecklistEntry("salah_isha", "نماز عشاء"),
        DailyChecklistEntry("rawatib_fajr", "سنت قبل از نماز صبح"),
        DailyChecklistEntry("rawatib_dhuhr_before", "سنت قبل از نماز ظهر"),
        DailyChecklistEntry("rawatib_dhuhr_after", "سنت بعد از نماز ظهر"),
        DailyChecklistEntry("rawatib_maghrib", "سنت بعد از نماز مغرب"),
        DailyChecklistEntry("rawatib_isha", "سنت بعد از نماز عشاء"),
        DailyChecklistEntry("morning_adhkar", "اذکار صبحگاه"),
        DailyChecklistEntry("quran", "تلاوت قرآن"),
        DailyChecklistEntry("duha", "نماز ضحی"),
        DailyChecklistEntry("charity", "صدقه"),
        DailyChecklistEntry("istighfar", "استغفار"),
        DailyChecklistEntry("salawat", "صلوات بر پیامبر (ص)"),
        DailyChecklistEntry("evening_adhkar", "اذکار شامگاه"),
        DailyChecklistEntry("witr_tahajjud", "نماز وتر و تهجد"),
        DailyChecklistEntry("sleep_adhkar", "اذکار خواب")
    )
}
