package com.example.calendar

/**
 * A fixed, offline occasion shown in the calendar. Religious entries carry a checkable [source];
 * wording stays close to the cited text and makes no regional or official claim.
 */
data class Occasion(
    val id: String,
    val title: String,
    val detail: String,
    val source: String?,
    val kind: Kind
) {
    enum class Kind { RELIGIOUS, CALENDAR }
}

/**
 * The occasion table. It is deliberately small: only dates on which the major sources agree.
 * Hijri rules follow the user's Hijri offset; Jalali rules never move.
 */
object Occasions {
    private sealed interface Rule {
        val occasion: Occasion
        fun matches(jalali: JalaliDate, hijri: HijriDate): Boolean
    }

    private class OnHijri(val month: Int, val days: IntRange, override val occasion: Occasion) : Rule {
        override fun matches(jalali: JalaliDate, hijri: HijriDate) = hijri.month == month && hijri.day in days
    }

    private class OnJalali(val month: Int, val day: Int, override val occasion: Occasion) : Rule {
        override fun matches(jalali: JalaliDate, hijri: HijriDate) = jalali.month == month && jalali.day == day
    }

    private fun religious(id: String, title: String, detail: String, source: String) =
        Occasion(id, title, detail, source, Occasion.Kind.RELIGIOUS)

    private val rules: List<Rule> = listOf(
        OnJalali(1, 1, Occasion(
            "nowruz", "نوروز، آغاز سال خورشیدی",
            "نخستین روز سال هجری شمسی.", null, Occasion.Kind.CALENDAR
        )),
        OnHijri(1, 1..1, religious(
            "hijri-new-year", "آغاز سال هجری قمری",
            "نخستین روز ماه محرم، یکی از چهار ماه حرام.",
            "قرآن کریم، سوره توبه، آیه ۳۶"
        )),
        OnHijri(1, 9..9, religious(
            "tasua", "تاسوعا",
            "پیامبر ﷺ قصد کردند روز نهم محرم را همراه با عاشورا روزه بگیرند.",
            "صحیح مسلم، حدیث ۱۱۳۴"
        )),
        OnHijri(1, 10..10, religious(
            "ashura", "عاشورا",
            "امید است روزه این روز کفاره گناهان یک سال گذشته باشد.",
            "صحیح مسلم، حدیث ۱۱۶۲"
        )),
        OnHijri(9, 1..1, religious(
            "ramadan-start", "آغاز ماه رمضان",
            "ماه روزه و ماه نزول قرآن.",
            "قرآن کریم، سوره بقره، آیه ۱۸۵"
        )),
        OnHijri(9, 20..20, religious(
            "ramadan-last-ten", "آغاز دهه آخر رمضان",
            "از غروب امروز دهه آخر رمضان آغاز می‌شود. شب قدر را در شب‌های فرد این دهه بجویید.",
            "صحیح بخاری، حدیث ۲۰۱۷"
        )),
        OnHijri(10, 1..1, religious(
            "eid-al-fitr", "عید فطر",
            "پایان ماه رمضان. روزه گرفتن در این روز نهی شده است.",
            "صحیح بخاری، حدیث ۱۹۹۰؛ صحیح مسلم، حدیث ۱۱۳۷"
        )),
        OnHijri(12, 1..1, religious(
            "dhul-hijjah-ten", "آغاز دهه اول ذی‌الحجه",
            "روزهایی که عمل نیک در آن‌ها نزد خداوند محبوب‌ترین است.",
            "صحیح بخاری، حدیث ۹۶۹"
        )),
        OnHijri(12, 9..9, religious(
            "arafah", "روز عرفه",
            "امید است روزه این روز کفاره گناهان سال گذشته و سال آینده باشد.",
            "صحیح مسلم، حدیث ۱۱۶۲"
        )),
        OnHijri(12, 10..10, religious(
            "eid-al-adha", "عید قربان",
            "روز قربانی. روزه گرفتن در این روز نهی شده است.",
            "صحیح بخاری، حدیث ۱۹۹۰؛ صحیح مسلم، حدیث ۱۱۳۷"
        )),
        OnHijri(12, 11..13, religious(
            "tashreeq", "ایام تشریق",
            "روزهای خوردن و نوشیدن و یاد خداوند.",
            "صحیح مسلم، حدیث ۱۱۴۱"
        ))
    )

    val all: List<Occasion> get() = rules.map { it.occasion }

    fun on(jalali: JalaliDate, hijri: HijriDate): List<Occasion> =
        rules.filter { it.matches(jalali, hijri) }.map { it.occasion }
}
