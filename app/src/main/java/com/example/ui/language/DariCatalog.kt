package com.example.ui.language

/**
 * Turns the app's Iranian Persian text into Afghan Dari where the two differ in written use:
 * solar month names (حمل … حوت), Gregorian month names, and a few everyday words.
 * Everything else is shared, so unmatched text passes through unchanged.
 *
 * Regexes avoid braces and Unicode property classes: Android's ICU parser rejects some forms the
 * desktop JVM accepts, so JVM unit tests alone cannot prove a pattern safe on device.
 */
object DariCatalog {
    private val solarMonths = listOf(
        "فروردین" to "حمل", "اردیبهشت" to "ثور", "خرداد" to "جوزا", "تیر" to "سرطان",
        "مرداد" to "اسد", "شهریور" to "سنبله", "مهر" to "میزان", "آبان" to "عقرب",
        "آذر" to "قوس", "دی" to "جدی", "بهمن" to "دلو", "اسفند" to "حوت"
    )

    /** Also ordinary words (arrow, kindness/seal, …): replaced only as a whole text or beside a number. */
    private val ambiguousMonths = setOf("تیر", "مهر", "دی")

    // «مه» (May) is also a word, so Gregorian names are replaced only when they are the whole text.
    private val gregorianMonths = listOf(
        "ژانویه" to "جنوری", "فوریه" to "فبروری", "مارس" to "مارچ", "آوریل" to "اپریل",
        "مه" to "می", "ژوئن" to "جون", "ژوئیه" to "جولای", "اوت" to "اگست",
        "سپتامبر" to "سپتمبر", "اکتبر" to "اکتوبر", "نوامبر" to "نومبر", "دسامبر" to "دسمبر"
    )

    private val words = listOf(
        "گوشی" to "موبایل"
    )

    private val literal: Map<String, String> = (solarMonths + gregorianMonths).toMap()

    // Arabic-script and Latin letters as plain ranges. Android's ICU rejects `\pL` (it crashed on
    // device although desktop JVM tests passed), so no Unicode property syntax here. ZWNJ (U+200C)
    // is outside these ranges, so «گوشی‌تان» still matches. The Arabic block's punctuation (، ؛ ؟ ۔)
    // and digits are left out so «اسفند،» and «۵ مهر» still count as whole words.
    private const val LETTER =
        "[ؠ-ٟٮ-ۓە-ۯۺ-ۿݐ-ݿﭐ-﷿ﹰ-﻿A-Za-z]"
    private const val LETTER_BEFORE = "(?<!$LETTER)"
    private const val LETTER_AFTER = "(?!$LETTER)"

    private val replacements: List<Pair<Regex, String>> = buildList {
        solarMonths.forEach { (persian, dari) ->
            val pattern = if (persian in ambiguousMonths) {
                // Beside a Persian or Latin number: «۵ مهر», «مهر ۱۴۰۵».
                "(?<=[0-9۰-۹] )$persian$LETTER_AFTER|$LETTER_BEFORE$persian(?= [0-9۰-۹])"
            } else {
                "$LETTER_BEFORE$persian$LETTER_AFTER"
            }
            add(Regex(pattern) to dari)
        }
        words.forEach { (persian, dari) -> add(Regex("$LETTER_BEFORE$persian$LETTER_AFTER") to dari) }
    }

    fun translate(text: String): String {
        literal[text.trim()]?.let { return text.replace(text.trim(), it) }
        return replacements.fold(text) { current, (pattern, dari) -> pattern.replace(current, dari) }
    }
}
