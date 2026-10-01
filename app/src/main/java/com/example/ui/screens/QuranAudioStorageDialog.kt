package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.media.*
import com.example.ui.language.AppLanguage
import com.example.ui.language.LocalAppLanguage
import com.example.ui.util.toPersianDigits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

internal fun audioSize(bytes: Long): String =
    if (bytes < 0) "—" else String.format(Locale.US, "%.1f MB", bytes / 1048576.0).toPersianDigits()

@Composable
fun QuranAudioStorageDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val arabic = LocalAppLanguage.current == AppLanguage.ARABIC
    val store = remember(context) { QuranAudioStore(context) }
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<StoredQuranAudio>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var deleting by remember { mutableStateOf(false) }
    var deletion by remember { mutableStateOf<Pair<String?, Int?>?>(null) }
    var error by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entries = withContext(Dispatchers.IO) { store.list() }
        loading = false
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (arabic) "التلاوات المحفوظة" else "تلاوت‌های دانلودشده") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text((if (arabic) "المساحة المستخدمة: " else "فضای استفاده‌شده: ") + audioSize(entries.sumOf { it.bytes }))
                if (loading) CircularProgressIndicator()
                else if (entries.isEmpty()) Text(if (arabic) "لا توجد تلاوات محفوظة. شغّل سورة لتنزيلها." else "هنوز تلاوتی دانلود نشده است. با پخش یک سوره، تلاوت آن ذخیره می‌شود.")
                if (error) Text(if (arabic) "تعذر الحذف. حاول مجددًا." else "حذف انجام نشد. دوباره تلاش کنید.", color = MaterialTheme.colorScheme.error)
                LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entries.groupBy { it.reciterId }.forEach { (id, files) ->
                        val reciter = QuranReciters.first { it.id == id }
                        item(key = id) {
                            Text(if (arabic) reciter.arName else reciter.faName, style = MaterialTheme.typography.titleSmall)
                            TextButton(enabled = !deleting, onClick = { deletion = id to null }) {
                                Text((if (arabic) "حذف تلاوات القارئ · " else "حذف تلاوت‌های قاری · ") + audioSize(files.sumOf { it.bytes }))
                            }
                        }
                        items(files, key = { "${it.reciterId}/${it.surah}" }) { entry ->
                            Row(Modifier.fillMaxWidth()) {
                                Text((if (arabic) "سورة " else "سورهٔ ") + entry.surah.toPersianDigits() + " · " + audioSize(entry.bytes), Modifier.weight(1f))
                                TextButton(enabled = !deleting, onClick = { deletion = entry.reciterId to entry.surah }) {
                                    Text(if (arabic) "حذف" else "حذف")
                                }
                            }
                        }
                    }
                }
                if (entries.isNotEmpty()) TextButton(enabled = !deleting, onClick = { deletion = null to null }) {
                    Text(if (arabic) "حذف الكل" else "حذف همه")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (arabic) "إغلاق" else "بستن") } }
    )
    deletion?.let { target ->
        AlertDialog(onDismissRequest = { if (!deleting) deletion = null },
            title = { Text(if (arabic) "حذف التلاوات؟" else "تلاوت‌ها حذف شوند؟") },
            text = { Text(if (arabic) "ستحتاج إلى الإنترنت لتنزيلها مرة أخرى." else "برای دانلود دوباره به اینترنت نیاز خواهید داشت.") },
            confirmButton = { TextButton(enabled = !deleting, onClick = {
                deleting = true
                QuranAudioPlayer.stop()
                scope.launch {
                    error = runCatching { withContext(Dispatchers.IO) { store.delete(target.first, target.second) } }.isFailure
                    entries = withContext(Dispatchers.IO) { store.list() }
                    deleting = false
                    deletion = null
                }
            }) { Text(if (arabic) "حذف" else "حذف") } },
            dismissButton = { TextButton(enabled = !deleting, onClick = { deletion = null }) { Text(if (arabic) "إلغاء" else "لغو") } }
        )
    }
}
