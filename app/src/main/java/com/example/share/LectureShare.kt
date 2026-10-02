package com.example.share

import com.example.data.model.Lecture
import com.example.data.model.Scholar
import com.example.ui.language.AppLanguage
import com.example.ui.language.text

/** The supplied summary without the backend's appended transcript section or heading. */
fun lectureSummary(lecture: Lecture): String {
    val description = lecture.description.replace("\r\n", "\n").trim()
    val transcriptHeader = Regex("(?m)^متن کامل سخنرانی[ \\t]*$").find(description)
    return (transcriptHeader?.let { description.substring(0, it.range.first) } ?: description)
        .replace(Regex("^خلاصه[ \\t]*\\n"), "").trim()
}

/** Share the supplied summary, excluding the backend's appended transcript section. */
fun lectureShareText(scholar: Scholar, lecture: Lecture, language: AppLanguage): String {
    val summary = lectureSummary(lecture)
    // Keep text sharing compact, including old descriptions that contain only a transcript.
    val excerpt = if (summary.length > 2_000) {
        summary.take(2_000).substringBeforeLast(' ').trimEnd() + "…"
    } else summary
    return buildString {
        append(language.text("عنوان سخنرانی:")); append(' '); append(lecture.title.trim())
        append('\n'); append(language.text("سخنران:")); append(' '); append(scholar.name.trim())
        append("\n\n"); append(language.text("خلاصهٔ سخنرانی:")); append('\n')
        append(excerpt.ifBlank { language.text("خلاصه‌ای برای این سخنرانی ثبت نشده است.") })
        append("\n\n"); append(appShareFooter(language))
    }
}
