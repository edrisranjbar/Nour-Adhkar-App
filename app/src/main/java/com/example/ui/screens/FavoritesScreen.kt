package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdhkarData
import com.example.ui.theme.AmiriQuran
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SandDark
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.TextArabic
import com.example.ui.theme.TextPersian
import com.example.ui.viewmodel.AdhkarViewModel

@Composable
fun FavoritesScreen(viewModel: AdhkarViewModel, innerPadding: PaddingValues) {
    val favoriteKeys by viewModel.favoriteDhikrKeys.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val favorites = AdhkarData.adhkarList.flatMap { (categoryId, items) ->
        items.filter { "$categoryId:${it.id}" in favoriteKeys }.map { Triple(categoryId, it, AdhkarData.categories.find { category -> category.id == categoryId }?.title.orEmpty()) }
    }

    if (favorites.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Favorite, null, tint = SoftBorder, modifier = Modifier.padding(8.dp))
                Text("هنوز موردی به علاقه‌مندی‌ها اضافه نشده است", color = NightBlue, textAlign = TextAlign.Center)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(favorites, key = { "${it.first}:${it.second.id}" }) { (categoryId, item, categoryTitle) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SoftBorder)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Box(Modifier.fillMaxWidth()) {
                        Text(categoryTitle, color = SandDark, fontSize = (12 * fontScale).sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterStart))
                        IconButton(onClick = { viewModel.toggleFavoriteDhikr(categoryId, item.id) }, modifier = Modifier.align(Alignment.CenterEnd)) {
                            Icon(Icons.Default.Favorite, "حذف از علاقه‌مندی‌ها", tint = androidx.compose.ui.graphics.Color(0xFFE05263))
                        }
                    }
                    Text(item.arabicText, color = TextArabic, fontFamily = AmiriQuran, fontWeight = FontWeight.Bold, fontSize = (19 * fontScale).sp, lineHeight = (34 * fontScale).sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(item.persianTranslation, color = TextPersian, fontSize = (13 * fontScale).sp, lineHeight = (21 * fontScale).sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
