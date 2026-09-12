package com.example.ui.screens
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.inLanguage
import com.example.ui.language.text

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.example.ui.language.LocalizedIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.example.ui.language.LocalizedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdhkarData
import com.example.data.model.ArticleItem
import com.example.ui.theme.SandDark
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.theme.TextArabic
import com.example.ui.theme.TextPersian
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel

@Composable
fun ArticlesScreen(
    viewModel: AdhkarViewModel,
    innerPadding: PaddingValues
) {
    val fontScale by viewModel.fontScale.collectAsState()
    val language = LocalAppLanguage.current
    val articles = AdhkarData.articles.map { it.inLanguage(language) }

    var expandedArticleId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = articles.firstOrNull { it.id == expandedArticleId }
    val listState = rememberLazyListState()
    BackHandler(enabled = selected != null) { expandedArticleId = null }
    if (selected != null) {
        ArticleDetailPage(selected, fontScale, innerPadding) { expandedArticleId = null }
        return
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp)
            ) {

                // Articles List
                items(articles, key = { it.id }) { article ->
                    val isExpanded = expandedArticleId == article.id
                    ArticleCard(
                        article = article,
                        isExpanded = isExpanded,
                        fontScale = fontScale,
                        onClick = {
                            expandedArticleId = if (isExpanded) null else article.id
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleDetailPage(article: ArticleItem, fontScale: Float, innerPadding: PaddingValues, onBack: () -> Unit) {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(top = innerPadding.calculateTopPadding())) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("بازگشت به مقالات") }
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, article.title)
                        putExtra(Intent.EXTRA_TEXT, "${article.title}\n\n${article.content}\n\n${language.text("اذکار نور")}")
                    }
                    context.startActivity(Intent.createChooser(intent, language.text("اشتراک‌گذاری مقاله")))
                }) { Icon(Icons.Default.Share, "اشتراک‌گذاری مقاله") }
            }
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 20.dp)) {
                item { Text(article.title.toPersianDigits(), fontSize = (22 * fontScale).sp,
                    lineHeight = (32 * fontScale).sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground) }
                item { Text(article.readTime.toPersianDigits(), fontSize = (12 * fontScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item { Text(article.content.toPersianDigits(), fontSize = (16 * fontScale).sp,
                    lineHeight = (28 * fontScale).sp, color = MaterialTheme.colorScheme.onBackground) }
            }
        }
    }
}

@Composable
fun ArticleCard(
    article: ArticleItem,
    isExpanded: Boolean,
    fontScale: Float,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SoftBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = article.readTime.toPersianDigits(),
                    fontSize = (10 * fontScale).sp,
                    color = SunGold,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = article.title.toPersianDigits(),
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = TextArabic,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Summary
            Text(
                text = article.summary.toPersianDigits(),
                fontSize = (12.5 * fontScale).sp,
                lineHeight = 18.sp,
                color = TextPersian,
                modifier = Modifier.fillMaxWidth()
            )

            // Expanded Full Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SoftBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = article.content.toPersianDigits(),
                        fontSize = (13.5 * fontScale).sp,
                        lineHeight = (22 * fontScale).sp,
                        color = TextArabic,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "بستن مقاله ↑",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SunGold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "ادامه مطلب ←",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SunGold,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
