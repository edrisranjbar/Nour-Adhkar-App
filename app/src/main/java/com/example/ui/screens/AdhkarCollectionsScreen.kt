package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.AdhkarViewModel

/** Remaining collections; morning/evening, sleep/daily, and Quran/Sunnah prayers stay on Home. */
@Composable
fun AdhkarCollectionsScreen(viewModel: AdhkarViewModel, innerPadding: PaddingValues) {
    val fontScale by viewModel.fontScale.collectAsState()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp)
        ) {
            item { CategoriesGrid(viewModel = viewModel, fontScale = fontScale) }
        }
    }
}
