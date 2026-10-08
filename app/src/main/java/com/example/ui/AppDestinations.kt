package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.AdhkarCollectionsScreen
import com.example.ui.screens.AppInboxScreen
import com.example.ui.screens.ArticlesScreen
import com.example.ui.screens.DailyChecklistScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QazaScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.QuranScreen
import com.example.ui.screens.ScholarsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasbihScreen
import com.example.ui.viewmodel.AdhkarViewModel

@Composable
fun AppDestinations(
    currentTab: String,
    viewModel: AdhkarViewModel,
    innerPadding: PaddingValues,
    openQuranPage: Int?,
    onQuranPageConsumed: () -> Unit,
    onOpenShortcut: (String) -> Unit,
    onAchievementsBack: () -> Unit,
    onOpenAchievementsFromAccount: () -> Unit,
    onOpenQuranAudio: () -> Unit
) {
    AnimatedContent(
        targetState = currentTab,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
        },
        label = "tabTransitions"
    ) { targetTab ->
        when (targetTab) {
            "home" -> HomeScreen(viewModel = viewModel, innerPadding = innerPadding,
                onOpenShortcut = onOpenShortcut)
            "checklist" -> DailyChecklistScreen(viewModel = viewModel, innerPadding = innerPadding)
            "tasbih" -> TasbihScreen(viewModel = viewModel, innerPadding = innerPadding)
            "achievements" -> AchievementsScreen(
                viewModel = viewModel,
                innerPadding = innerPadding,
                onNavigateBack = onAchievementsBack
            )
            "about" -> AboutScreen(viewModel = viewModel, innerPadding = innerPadding)
            "app_inbox" -> AppInboxScreen(innerPadding = innerPadding)
            "account" -> com.example.ui.screens.AccountScreen(
                innerPadding = innerPadding,
                onOpenAchievements = onOpenAchievementsFromAccount,
                onContinueWithoutAccount = { viewModel.selectTab("home") }
            )
            "scholars" -> ScholarsScreen(viewModel = viewModel, innerPadding = innerPadding)
            "articles" -> ArticlesScreen(viewModel = viewModel, innerPadding = innerPadding)
            "adhkar" -> AdhkarCollectionsScreen(viewModel = viewModel, innerPadding = innerPadding)
            "quran" -> QuranScreen(
                innerPadding = innerPadding,
                onNavigateHome = { viewModel.selectTab("home") },
                onOpenAudio = onOpenQuranAudio,
                requestedPage = openQuranPage,
                onRequestedPageConsumed = onQuranPageConsumed
            )
            "qibla" -> QiblaScreen(viewModel = viewModel, innerPadding = innerPadding)
            "quran_audio" -> com.example.ui.screens.QuranAudioScreen(viewModel = viewModel, innerPadding = innerPadding)
            "qaza" -> QazaScreen(viewModel = viewModel, innerPadding = innerPadding)
            "calendar" -> com.example.ui.screens.CalendarScreen(viewModel = viewModel, innerPadding = innerPadding)
            "stats" -> com.example.ui.screens.StatsScreen(viewModel = viewModel, innerPadding = innerPadding)
            "favorites" -> FavoritesScreen(viewModel = viewModel, innerPadding = innerPadding)
            "settings" -> SettingsScreen(viewModel = viewModel, innerPadding = innerPadding)
            else -> HomeScreen(viewModel = viewModel, innerPadding = innerPadding)
        }
    }
}

// Simple tween container helper
private fun <T> tween(duration: Int) = androidx.compose.animation.core.tween<T>(duration)
