package com.example.widget
import com.example.ui.language.text
import com.example.data.repository.PreferenceRepository

import android.content.Context
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.TypefaceSpan
import androidx.core.content.res.ResourcesCompat
import com.example.R

internal object WidgetTypography {
    fun vazirmatn(context: Context, text: CharSequence, bold: Boolean = false): CharSequence {
        val localized = PreferenceRepository(context).getAppLanguage().text(text.toString())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return localized
        val font = ResourcesCompat.getFont(
            context,
            if (bold) R.font.vazirmatn_bold else R.font.vazirmatn_regular
        ) ?: return localized
        return SpannableString(localized).apply {
            setSpan(TypefaceSpan(font), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
