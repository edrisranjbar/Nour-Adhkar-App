package com.example.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.example.ui.language.AppLanguage
import com.example.ui.language.text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Store links and feedback address shared by every outward-facing share and prompt. */
object AppLinks {
    const val BAZAAR_WEB_URL = "https://cafebazaar.ir/app/ir.adhkar.app"
    const val BAZAAR_DETAILS_URI = "bazaar://details?id=ir.adhkar.app"
    const val BAZAAR_PACKAGE = "com.farsitel.bazaar"
    const val FEEDBACK_EMAIL = "edrisranjbar.dev@gmail.com"
}

/** Footer appended to shared text so recipients can find and install the app. */
fun appShareFooter(language: AppLanguage): String =
    language.text("اذکار نور") + "\n" +
        language.text("دریافت رایگان از کافه‌بازار:") + "\n" +
        AppLinks.BAZAAR_WEB_URL

fun shareAppText(context: Context, text: String, chooserTitle: String, subject: String? = null) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

/**
 * Renders [spec] off the main thread and opens the share chooser with the image and [caption].
 * Apps that drop the caption still show the store link printed on the card.
 */
suspend fun shareAppCardImage(context: Context, spec: ShareCardSpec, caption: String, chooserTitle: String) {
    val uri = withContext(Dispatchers.Default) {
        val bitmap = ShareCardRenderer.render(context, spec)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "nour_adhkar_share.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    )
}
