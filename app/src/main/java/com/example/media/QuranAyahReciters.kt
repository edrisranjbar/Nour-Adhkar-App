package com.example.media

/**
 * An ayah-by-ayah Hafs recitation from EveryAyah: one MP3 per verse at `<folder>/<SSSAAA>.mp3`.
 * [mirrored] folders are also served under the same name by QuranicAudio's EveryAyah mirror,
 * which is tried when the primary host fails.
 */
data class QuranAyahReciter(
    val id: String,
    val faName: String,
    val arName: String,
    val folder: String,
    val mirrored: Boolean = false
) {
    fun ayahUrls(surah: Int, ayah: Int): List<String> {
        require(surah in 1..114 && ayah >= 1)
        val file = "${surah.toString().padStart(3, '0')}${ayah.toString().padStart(3, '0')}.mp3"
        return listOfNotNull(
            "$EVERY_AYAH/$folder/$file",
            "$EVERY_AYAH_MIRROR/$folder/$file".takeIf { mirrored }
        )
    }

    companion object {
        const val EVERY_AYAH = "https://everyayah.com/data"
        const val EVERY_AYAH_MIRROR = "https://mirrors.quranicaudio.com/everyayah"
    }
}

/**
 * Curated complete Hafs recitations; not a popularity ranking.
 * Source: https://everyayah.com/data/recitations.js (checked 2026-10-06). Each folder's 1:1, 114:6,
 * 2:282 and several mid-Quran verses returned HTTP 200; Mustafa Ismail was left out because many
 * verses are missing. IDs match [QuranReciters] for the same Qari so an existing choice carries over.
 */
val QuranAyahReciters = listOf(
    QuranAyahReciter("afs", "مشاری راشد العفاسی", "مشاري العفاسي", "Alafasy_128kbps", mirrored = true),
    QuranAyahReciter("basit", "عبدالباسط عبدالصمد (مرتل)", "عبدالباسط عبدالصمد (مرتل)", "Abdul_Basit_Murattal_192kbps", mirrored = true),
    QuranAyahReciter("basit_mujawwad", "عبدالباسط عبدالصمد (مجوّد)", "عبدالباسط عبدالصمد (مجوّد)", "Abdul_Basit_Mujawwad_128kbps", mirrored = true),
    QuranAyahReciter("sds", "عبدالرحمن السدیس", "عبدالرحمن السديس", "Abdurrahmaan_As-Sudais_192kbps", mirrored = true),
    QuranAyahReciter("shur", "سعود الشریم", "سعود الشريم", "Saood_ash-Shuraym_128kbps", mirrored = true),
    QuranAyahReciter("husr", "محمود خلیل الحصری", "محمود خليل الحصري", "Husary_128kbps", mirrored = true),
    QuranAyahReciter("husr_muallim", "محمود خلیل الحصری (معلم)", "محمود خليل الحصري (المعلم)", "Husary_Muallim_128kbps", mirrored = true),
    QuranAyahReciter("minsh", "محمد صدیق المنشاوی (مرتل)", "محمد صديق المنشاوي (مرتل)", "Minshawy_Murattal_128kbps", mirrored = true),
    QuranAyahReciter("minsh_mujawwad", "محمد صدیق المنشاوی (مجوّد)", "محمد صديق المنشاوي (مجوّد)", "Minshawy_Mujawwad_192kbps", mirrored = true),
    QuranAyahReciter("maher", "ماهر المعیقلی", "ماهر المعيقلي", "MaherAlMuaiqly128kbps"),
    QuranAyahReciter("s_gmd", "سعد الغامدی", "سعد الغامدي", "Ghamadi_40kbps", mirrored = true),
    QuranAyahReciter("ajm", "احمد العجمی", "أحمد بن علي العجمي", "ahmed_ibn_ali_al_ajamy_128kbps"),
    QuranAyahReciter("yasser", "یاسر الدوسری", "ياسر الدوسري", "Yasser_Ad-Dussary_128kbps", mirrored = true),
    QuranAyahReciter("mp3quran_4", "ابوبکر الشاطری", "أبو بكر الشاطري", "Abu_Bakr_Ash-Shaatree_128kbps", mirrored = true),
    QuranAyahReciter("mp3quran_86", "ناصر القطامی", "ناصر القطامي", "Nasser_Alqatami_128kbps"),
    QuranAyahReciter("mp3quran_81", "فارس عباد", "فارس عباد", "Fares_Abbad_64kbps"),
    QuranAyahReciter("mp3quran_89", "هانی الرفاعی", "هاني الرفاعي", "Hani_Rifai_192kbps", mirrored = true),
    QuranAyahReciter("mp3quran_74", "علی الحذیفی", "علي بن عبدالرحمن الحذيفي", "Hudhaify_128kbps", mirrored = true),
    QuranAyahReciter("mp3quran_109", "محمد ایوب", "محمد أيوب", "Muhammad_Ayyoub_128kbps", mirrored = true),
    QuranAyahReciter("mp3quran_111", "محمد جبریل", "محمد جبريل", "Muhammad_Jibreel_128kbps", mirrored = true),
    QuranAyahReciter("mp3quran_106", "محمد الطبلاوی", "محمد الطبلاوي", "Mohammad_al_Tablaway_128kbps", mirrored = true),
    QuranAyahReciter("mp3quran_60", "عبدالله بصفر", "عبدالله بصفر", "Abdullah_Basfar_192kbps", mirrored = true),
    QuranAyahReciter("mp3quran_43", "صلاح البدیر", "صلاح البدير", "Salah_Al_Budair_128kbps"),
    QuranAyahReciter("mp3quran_46", "صلاح بوخاطر", "صلاح بو خاطر", "Salaah_AbdulRahman_Bukhatir_128kbps"),
    QuranAyahReciter("mp3quran_62", "عبدالله عواد الجهنی", "عبدالله عواد الجهني", "Abdullaah_3awwaad_Al-Juhaynee_128kbps"),
    QuranAyahReciter("mp3quran_67", "عبدالمحسن القاسم", "عبدالمحسن القاسم", "Muhsin_Al_Qasim_192kbps"),
    QuranAyahReciter("mp3quran_59", "عبدالله المطرود", "عبدالله المطرود", "Abdullah_Matroud_128kbps"),
    QuranAyahReciter("mp3quran_76", "علی جابر", "علي جابر", "Ali_Jaber_64kbps"),
    QuranAyahReciter("qahtani", "خالد القحطانی", "خالد القحطاني", "Khaalid_Abdullaah_al-Qahtaanee_192kbps"),
    QuranAyahReciter("parhizgar", "شهریار پرهیزگار", "شهريار پرهيزگار", "Parhizgar_48kbps"),
    QuranAyahReciter("mansoori", "کریم منصوری", "كريم منصوري", "Karim_Mansoori_40kbps"),
)

/** The saved ayah voice, else the reader's surah voice when it has an ayah recording, else Alafasy. */
fun quranAyahReciter(savedAyahId: String?, surahReciterId: String?): QuranAyahReciter =
    QuranAyahReciters.firstOrNull { it.id == savedAyahId }
        ?: QuranAyahReciters.firstOrNull { it.id == surahReciterId }
        ?: QuranAyahReciters.first()
