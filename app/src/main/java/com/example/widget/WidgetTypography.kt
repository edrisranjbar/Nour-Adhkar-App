package com.example.widget

import android.content.Context
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.TypefaceSpan
import androidx.core.content.res.ResourcesCompat
import com.example.R

internal object WidgetTypography {
    fun vazirmatn(context: Context, text: CharSequence, bold: Boolean = false): CharSequence {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return text
        val font = ResourcesCompat.getFont(
            context,
            if (bold) R.font.vazirmatn_bold else R.font.vazirmatn_regular
        ) ?: return text
        return SpannableString(text).apply {
            setSpan(TypefaceSpan(font), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
