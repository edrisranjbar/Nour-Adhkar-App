package com.example.voice

private val arabicMarks = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
private val nonLettersOrNumbers = Regex("[^\\p{L}\\p{N}]+")

fun normalizeDhikr(text: String): String = text
    .replace(arabicMarks, "")
    .replace("ـ", "")
    .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
    .replace('ى', 'ي').replace('ۀ', 'ه').replace('ة', 'ه')
    .replace('ؤ', 'و').replace('ئ', 'ي')
    .replace(nonLettersOrNumbers, " ")
    .trim().replace(Regex("\\s+"), " ")

fun countCompleteDhikr(hypothesis: String, dhikr: String): Int {
    val spoken = normalizeDhikr(hypothesis).split(' ').filter(String::isNotBlank)
    val target = normalizeDhikr(dhikr).split(' ').filter(String::isNotBlank)
    if (spoken.isEmpty() || target.isEmpty() || spoken.size < target.size) return 0
    var matches = 0
    var index = 0
    while (index <= spoken.size - target.size) {
        if (spoken.subList(index, index + target.size) == target) {
            matches++
            index += target.size
        } else index++
    }
    return matches
}
