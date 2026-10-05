package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.AppInboxApi
import com.example.data.repository.MyFeedback
import com.example.data.repository.ServerException
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text
import com.example.ui.util.formatInboxDate
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text

private val LikeRed = Color(0xFFD23C5A)

/**
 * «پیشنهادهای من»: the signed-in user's feedback, newest first, each with the team's like
 * («پسندیده شد») and reply. Replies the user had not seen before are marked «پاسخ تازه».
 */
@Composable
fun MyFeedbackList(typeLabels: Map<String, String>, refreshKey: Int) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    var items by remember { mutableStateOf<List<MyFeedback>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    // Captured before marking as seen, so this visit still highlights what is new.
    var unseen by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(refreshKey, retry) {
        error = null
        try {
            val loaded = AppInboxApi.myFeedback(context)
            val seen = AppInboxApi.seenReplies(context)
            unseen = loaded.mapNotNull { it.replyKey }.filterNot { it in seen }.toSet()
            items = loaded
            AppInboxApi.markRepliesSeen(context, loaded)
        } catch (e: ServerException) {
            error = if (e.code == 401) "نشست شما منقضی شده است. لطفاً دوباره وارد حساب شوید."
            else "دریافت پیام‌ها ممکن نشد. لطفاً بعداً دوباره تلاش کنید."
        } catch (e: Exception) {
            error = "اتصال به سرور برقرار نشد. اینترنت را بررسی کنید."
        }
    }

    val list = items
    when {
        list == null && error == null -> Box(Modifier.fillMaxWidth().heightIn(min = 120.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp)
        }
        error != null && list == null -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(error!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { retry++ }) { Text("تلاش دوباره") }
        }
        list.isNullOrEmpty() -> Text(
            "هنوز پیامی نفرستاده‌اید. نظر یا پیشنهاد خود را از بخش «ارسال پیام» بفرستید.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        else -> Column(
            Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            list.forEach { item -> FeedbackCard(item, typeLabels[item.type] ?: item.type, item.replyKey in unseen, language) }
        }
    }
}

@Composable
private fun FeedbackCard(item: MyFeedback, typeLabel: String, newReply: Boolean, language: com.example.ui.language.AppLanguage) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50), color = colors.secondaryContainer) {
                    Text(typeLabel, style = MaterialTheme.typography.labelMedium, color = colors.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(formatInboxDate(item.createdAt), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f))
                if (item.liked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = language.text("تیم اذکار نور این پیام را پسندید") }
                    ) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, tint = LikeRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("پسندیده شد", style = MaterialTheme.typography.labelSmall, color = LikeRed)
                    }
                }
            }
            Text(item.message, style = MaterialTheme.typography.bodyMedium)
            item.reply?.let { reply ->
                Surface(shape = RoundedCornerShape(12.dp), color = colors.primaryContainer.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SupportAgent, contentDescription = null, tint = colors.tertiary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("پاسخ تیم اذکار نور", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                                color = colors.onPrimaryContainer, modifier = Modifier.weight(1f))
                            if (newReply) {
                                Surface(shape = RoundedCornerShape(50), color = colors.tertiary) {
                                    Text("پاسخ تازه", style = MaterialTheme.typography.labelSmall, color = colors.surface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text(reply, style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
                        item.repliedAt?.let {
                            Text(formatInboxDate(it), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            } ?: Text(
                if (item.liked) "پیام شما خوانده و پسندیده شد." else "پیام شما ثبت شده است.",
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant
            )
        }
    }
}
