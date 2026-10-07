package com.example.ui.language

/**
 * An offline phrase table keyed by the app's Persian source text, one `persian|target` pair per
 * line. Keys containing `{0}`, `{1}` … are templates whose captured parts are translated
 * recursively and may be reordered in the target.
 *
 * Regexes escape both braces: Android's ICU parser rejects an unescaped `}` that desktop JVM tests
 * accept. Digits are compared in Latin form and rendered with [localizeDigits].
 */
internal class PhraseCatalog(source: String, private val localizeDigits: (String) -> String) {
    private val entries: List<Pair<String, String>> = source.trimIndent().lineSequence()
        .filter { it.contains('|') }
        .map { val (key, target) = it.split('|', limit = 2); key to target }
        .toList()

    /** Source keys exactly as written, for coverage tests. */
    val keys: Set<String> get() = entries.mapTo(LinkedHashSet()) { it.first }

    private val literal = entries.filterNot { it.first.contains("{0}") }
        .associate { latinDigits(it.first) to it.second }
    private val patterns = entries.filter { it.first.contains("{0}") }
        .sortedByDescending { it.first.length }
        .map { (key, target) ->
            val parts = latinDigits(key).split(PLACEHOLDER)
            Regex(parts.joinToString("(.*?)") { Regex.escape(it) }, RegexOption.DOT_MATCHES_ALL) to target
        }

    fun translate(text: String, depth: Int = 0): String {
        val normalized = latinDigits(text)
        literal[normalized]?.let { return localizeDigits(it) }
        if (depth < 4) patterns.forEach { (pattern, target) ->
            val match = pattern.matchEntire(normalized) ?: return@forEach
            return localizeDigits(PLACEHOLDER_GROUP.replace(target) {
                translate(match.groupValues[it.groupValues[1].toInt() + 1], depth + 1)
            })
        }
        return localizeDigits(text)
    }

    companion object {
        private val PLACEHOLDER = Regex("\\{[0-9]+\\}")
        private val PLACEHOLDER_GROUP = Regex("\\{([0-9]+)\\}")

        fun latinDigits(text: String) = text.map {
            when (it) { in '۰'..'۹' -> '0' + (it - '۰'); in '٠'..'٩' -> '0' + (it - '٠'); else -> it }
        }.joinToString("")

        fun arabicIndicDigits(text: String) = latinDigits(text).map {
            if (it in '0'..'9') '٠' + (it - '0') else it
        }.joinToString("")

        /** Extended Arabic-Indic digits (U+06F0…), shared by Persian and Urdu text. */
        fun extendedArabicIndicDigits(text: String) = latinDigits(text).map {
            if (it in '0'..'9') '۰' + (it - '0') else it
        }.joinToString("")
    }
}
