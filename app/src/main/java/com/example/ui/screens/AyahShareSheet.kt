package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.quran.QuranVerse
import com.example.share.ShareCardFit
import com.example.share.ShareCardRenderer
import com.example.share.ayahShareCard
import com.example.share.ayahShareText
import com.example.share.shareAppCardImage
import com.example.share.shareAppText
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text

/**
 * «اشتراک‌گذاری آیه»: share one verse as an image card or as text, with or without its translation.
 * An image never cuts Quran text: when the verse is too long for the card, the translation is left
 * off the image, and for the longest verses text sharing is recommended instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahShareSheet(
    verse: QuranVerse,
    /** The verse's translation in the reader's current translation, or blank when unavailable. */
    translation: String?,
    translationCredit: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    val hasTranslation = !translation.isNullOrBlank()
    var asImage by rememberSaveable(verse.id) { mutableStateOf(true) }
    var withTranslation by rememberSaveable(verse.id) { mutableStateOf(hasTranslation) }
    var sharing by remember { mutableStateOf(false) }

    // How the verse fits on the card with the chosen options.
    val fit by produceState<ShareCardFit?>(null, verse.id, withTranslation, language) {
        value = withContext(Dispatchers.Default) {
            ShareCardRenderer.fit(context, ayahShareCard(verse, translation.takeIf { withTranslation }, language))
        }
    }
    val imageTooLong = fit == ShareCardFit.TRUNCATED
    val translationDropped = withTranslation && fit == ShareCardFit.WITHOUT_BODY
    // A verse too long even for the Arabic-only card is shared as text.
    LaunchedEffect(imageTooLong) { if (imageTooLong) asImage = false }

    val preview by produceState<ImageBitmap?>(null, verse.id, withTranslation, asImage, fit, language) {
        value = null
        if (!asImage || fit == null || imageTooLong) return@produceState
        value = withContext(Dispatchers.Default) {
            val spec = ayahShareCard(verse, translation.takeIf { withTranslation && !translationDropped }, language)
            ShareCardRenderer.render(context, spec).asImageBitmap()
        }
    }
    val text = remember(verse.id, withTranslation, language) {
        ayahShareText(verse, translation.takeIf { withTranslation }, translationCredit, language)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("اشتراک‌گذاری آیه", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !asImage, onClick = { asImage = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = { Icon(Icons.Rounded.TextFields, contentDescription = null, modifier = Modifier.size(18.dp)) }
                ) { Text("متن") }
                SegmentedButton(
                    selected = asImage, onClick = { asImage = true }, enabled = !imageTooLong,
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = { Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp)) }
                ) { Text("تصویر") }
            }

            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics(mergeDescendants = true) { role = Role.Switch },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("همراه با ترجمه", style = MaterialTheme.typography.titleSmall)
                    if (!hasTranslation) {
                        Text("ترجمه این آیه در دسترس نیست.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(checked = withTranslation && hasTranslation, onCheckedChange = { withTranslation = it }, enabled = hasTranslation)
            }

            when {
                imageTooLong -> Note("این آیه برای تصویر بلند است؛ به‌صورت متن به اشتراک گذاشته می‌شود.")
                asImage && translationDropped -> Note("ترجمه در تصویر جا نمی‌شود؛ تصویر فقط متن آیه را دارد و ترجمه همراه پیام فرستاده می‌شود.")
            }

            // Preview: the actual card, or the exact text that will be sent.
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                if (asImage) {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        val bitmap = preview
                        if (bitmap == null) {
                            Box(Modifier.fillMaxWidth(0.7f).aspectRatio(4f / 5f), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp)
                            }
                        } else {
                            Image(
                                bitmap = bitmap,
                                contentDescription = language.text("پیش‌نمایش تصویر آیه"),
                                modifier = Modifier.fillMaxWidth(0.7f).aspectRatio(4f / 5f).clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                } else {
                    Text(
                        text, modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start
                    )
                }
            }

            Button(
                onClick = {
                    sharing = true
                    scope.launch {
                        try {
                            if (asImage && !imageTooLong) {
                                val spec = ayahShareCard(verse, translation.takeIf { withTranslation && !translationDropped }, language)
                                // The caption carries the verse as text too, including a translation that did not fit the card.
                                shareAppCardImage(context, spec, caption = text, chooserTitle = language.text("اشتراک‌گذاری آیه"))
                            } else {
                                shareAppText(context, text, chooserTitle = language.text("اشتراک‌گذاری آیه"))
                            }
                            onDismiss()
                        } finally {
                            sharing = false
                        }
                    }
                },
                enabled = !sharing && (!asImage || preview != null),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (sharing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("اشتراک‌گذاری")
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}
