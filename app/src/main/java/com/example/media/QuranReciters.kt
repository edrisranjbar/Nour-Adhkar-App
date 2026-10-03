package com.example.media

/** A complete Hafs recitation, selected from MP3Quran's public catalog. */
data class QuranReciter(val id: String, val faName: String, val arName: String, val baseUrl: String) {
    fun surahUrl(surah: Int): String {
        require(surah in 1..114)
        return "$baseUrl/${surah.toString().padStart(3, '0')}.mp3"
    }
}

/** Curated catalog of 50 complete recitations; not an asserted popularity ranking.
 * Source: https://www.mp3quran.net/api/v3/reciters?language=ar (2026-10-03).
 * The first ten stable IDs and URLs preserve existing downloads and preferences. */
val QuranReciters = listOf(
    QuranReciter("afs", "مشاری راشد العفاسی", "مشاري العفاسي", "https://server8.mp3quran.net/afs"),
    QuranReciter("basit", "عبدالباسط عبدالصمد", "عبدالباسط عبدالصمد", "https://server7.mp3quran.net/basit"),
    QuranReciter("sds", "عبدالرحمن السدیس", "عبدالرحمن السديس", "https://server11.mp3quran.net/sds"),
    QuranReciter("shur", "سعود الشریم", "سعود الشريم", "https://server7.mp3quran.net/shur"),
    QuranReciter("husr", "محمود خلیل الحصری", "محمود خليل الحصري", "https://server13.mp3quran.net/husr"),
    QuranReciter("minsh", "محمد صدیق المنشاوی", "محمد صديق المنشاوي", "https://server10.mp3quran.net/minsh"),
    QuranReciter("maher", "ماهر المعیقلی", "ماهر المعيقلي", "https://server12.mp3quran.net/maher"),
    QuranReciter("s_gmd", "سعد الغامدی", "سعد الغامدي", "https://server7.mp3quran.net/s_gmd"),
    QuranReciter("ajm", "احمد العجمی", "أحمد بن علي العجمي", "https://server10.mp3quran.net/ajm"),
    QuranReciter("yasser", "یاسر الدوسری", "ياسر الدوسري", "https://server11.mp3quran.net/yasser"),
    QuranReciter("mp3quran_4", "شیخ أبو بکر الشاطری", "شيخ أبو بكر الشاطري", "https://server11.mp3quran.net/shatri"),
    QuranReciter("mp3quran_81", "فارس عباد", "فارس عباد", "https://server8.mp3quran.net/frs_a"),
    QuranReciter("mp3quran_86", "ناصر القطامی", "ناصر القطامي", "https://server6.mp3quran.net/qtm"),
    QuranReciter("mp3quran_20", "خالد الجلیل", "خالد الجليل", "https://server10.mp3quran.net/jleel"),
    QuranReciter("mp3quran_12", "إدریس أبکر", "إدريس أبكر", "https://server6.mp3quran.net/abkr"),
    QuranReciter("mp3quran_219", "ودیع الیمنی", "وديع اليمني", "https://server6.mp3quran.net/wdee3"),
    QuranReciter("mp3quran_221", "رعد محمد الکردی", "رعد محمد الكردي", "https://server6.mp3quran.net/kurdi"),
    QuranReciter("mp3quran_225", "عبدالرحمن العوسی", "عبدالرحمن العوسي", "https://server6.mp3quran.net/aloosi"),
    QuranReciter("mp3quran_245", "منصور السالمی", "منصور السالمي", "https://server14.mp3quran.net/mansor"),
    QuranReciter("mp3quran_217", "بندر بلیله", "بندر بليله", "https://server6.mp3quran.net/balilah"),
    QuranReciter("mp3quran_74", "علی بن عبدالرحمن الحذیفی", "علي بن عبدالرحمن الحذيفي", "https://server9.mp3quran.net/hthfi"),
    QuranReciter("mp3quran_76", "علی جابر", "علي جابر", "https://server11.mp3quran.net/a_jbr"),
    QuranReciter("mp3quran_60", "عبدالله بصفر", "عبدالله بصفر", "https://server6.mp3quran.net/bsfr"),
    QuranReciter("mp3quran_62", "عبدالله عواد الجهنی", "عبدالله عواد الجهني", "https://server13.mp3quran.net/jhn"),
    QuranReciter("mp3quran_67", "عبدالمحسن القاسم", "عبدالمحسن القاسم", "https://server8.mp3quran.net/qasm"),
    QuranReciter("mp3quran_43", "صلاح البدیر", "صلاح البدير", "https://server6.mp3quran.net/s_bud"),
    QuranReciter("mp3quran_46", "صلاح بو خاطر", "صلاح بو خاطر", "https://server8.mp3quran.net/bu_khtr"),
    QuranReciter("mp3quran_111", "محمد جبریل", "محمد جبريل", "https://server8.mp3quran.net/jbrl"),
    QuranReciter("mp3quran_109", "محمد أیوب", "محمد أيوب", "https://server16.mp3quran.net/ayyoub2/Rewayat-Hafs-A-n-Assem"),
    QuranReciter("mp3quran_121", "محمود علی البنا", "محمود علي البنا", "https://server8.mp3quran.net/bna"),
    QuranReciter("mp3quran_125", "مصطفى إسماعیل", "مصطفى إسماعيل", "https://server8.mp3quran.net/mustafa"),
    QuranReciter("mp3quran_106", "محمد الطبلاوی", "محمد الطبلاوي", "https://server12.mp3quran.net/tblawi"),
    QuranReciter("mp3quran_107", "محمد اللحیدان", "محمد اللحيدان", "https://server8.mp3quran.net/lhdan"),
    QuranReciter("mp3quran_108", "محمد المحیسنی", "محمد المحيسني", "https://server11.mp3quran.net/mhsny"),
    QuranReciter("mp3quran_1", "إبراهیم الأخضر", "إبراهيم الأخضر", "https://server6.mp3quran.net/akdr"),
    QuranReciter("mp3quran_24", "خلیفة الطنیجی", "خليفة الطنيجي", "https://server12.mp3quran.net/tnjy"),
    QuranReciter("mp3quran_17", "توفیق الصایغ", "توفيق الصايغ", "https://server6.mp3quran.net/twfeeq"),
    QuranReciter("mp3quran_13", "الزین محمد أحمد", "الزين محمد أحمد", "https://server9.mp3quran.net/alzain"),
    QuranReciter("mp3quran_38", "شیرزاد عبدالرحمن طاهر", "شيرزاد عبدالرحمن طاهر", "https://server12.mp3quran.net/taher"),
    QuranReciter("mp3quran_259", "أحمد النفیس", "أحمد النفيس", "https://server16.mp3quran.net/nufais/Rewayat-Hafs-A-n-Assem"),
    QuranReciter("mp3quran_267", "عبدالله کامل", "عبدالله كامل", "https://server16.mp3quran.net/kamel/Rewayat-Hafs-A-n-Assem"),
    QuranReciter("mp3quran_286", "حسن صالح", "حسن صالح", "https://server16.mp3quran.net/h_saleh/Rewayat-Hafs-A-n-Assem"),
    QuranReciter("mp3quran_273", "هیثم الدخین", "هيثم الدخين", "https://server16.mp3quran.net/h_dukhain/Rewayat-Hafs-A-n-Assem"),
    QuranReciter("mp3quran_268", "بیشه وا قادر الکردی", "بيشه وا قادر الكردي", "https://server16.mp3quran.net/peshawa/Rewayat-Hafs-A-n-Assem"),
    QuranReciter("mp3quran_87", "نبیل الرفاعی", "نبيل الرفاعي", "https://server9.mp3quran.net/nabil"),
    QuranReciter("mp3quran_89", "هانی الرفاعی", "هاني الرفاعي", "https://server8.mp3quran.net/hani"),
    QuranReciter("mp3quran_59", "عبدالله المطرود", "عبدالله المطرود", "https://server8.mp3quran.net/mtrod"),
    QuranReciter("mp3quran_48", "عادل ریان", "عادل ريان", "https://server8.mp3quran.net/ryan"),
    QuranReciter("mp3quran_55", "عبدالعزیز الأحمد", "عبدالعزيز الأحمد", "https://server11.mp3quran.net/a_ahmed"),
    QuranReciter("mp3quran_159", "خالد المهنا", "خالد المهنا", "https://server11.mp3quran.net/mohna"),
)
