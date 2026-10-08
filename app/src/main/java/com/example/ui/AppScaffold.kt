package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.BuildConfig
import com.example.store.StoreIntents
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text
import com.example.ui.screens.DhikrCounterScreen
import com.example.ui.viewmodel.AdhkarViewModel
import com.example.updates.AppUpdate
import com.example.updates.UpdateAvailableBottomSheet
import com.example.updates.UpdateChecker
import kotlinx.coroutines.launch

@Composable
fun AppMainScaffold(
    viewModel: AdhkarViewModel,
    onExitRequested: () -> Unit,
    notificationCategory: String? = null,
    onNotificationCategoryConsumed: () -> Unit = {},
    openChecklistFromWidget: Boolean = false,
    onChecklistWidgetIntentConsumed: () -> Unit = {},
    openQuranPage: Int? = null,
    onQuranPageConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    // Where the achievements screen returns to: Home (app bar icon) or Profile (banner).
    var achievementsBackTab by remember { mutableStateOf("account") }
    var quranAudioBackTab by remember { mutableStateOf("home") }
    val language = LocalAppLanguage.current
    val currentTab by viewModel.currentTab.collectAsState()
    LaunchedEffect(context) { com.example.data.repository.AccountRepository.init(context) }
    val accountUser by com.example.data.repository.AccountRepository.user.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    var availableUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(notificationCategory) {
        val category = notificationCategory
        if (category != null && category in setOf("morning", "evening", "after_salah")) {
            viewModel.selectCategory(category)
            onNotificationCategoryConsumed()
        }
    }

    LaunchedEffect(openChecklistFromWidget) {
        if (openChecklistFromWidget) {
            viewModel.selectTab("checklist")
            onChecklistWidgetIntentConsumed()
        }
    }

    LaunchedEffect(openQuranPage) {
        if (openQuranPage != null) viewModel.selectTab("quran")
    }

    // Back steps out of nested tabs to Home; on Home a second press within 2s exits.
    var lastHomeBackAt by remember(currentTab, selectedCategoryId, drawerState.isOpen) {
        mutableStateOf<Long?>(null)
    }
    BackHandler {
        when {
            drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
            currentTab == "achievements" -> viewModel.selectTab(achievementsBackTab)
            currentTab == "quran_audio" -> viewModel.selectTab(quranAudioBackTab)
            currentTab != "home" -> viewModel.selectTab("home")
            else -> {
                val now = android.os.SystemClock.elapsedRealtime()
                val previousBack = lastHomeBackAt
                if (previousBack != null && now - previousBack < 2000L) {
                    lastHomeBackAt = null
                    onExitRequested()
                } else {
                    lastHomeBackAt = now
                    android.widget.Toast.makeText(context, language.text("برای خروج دوباره بزنید"), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Warm the articles cache so «مقالات» opens instantly, even the first time.
    LaunchedEffect(Unit) { com.example.data.repository.ArticlesRepository.prefetch(context) }

    LaunchedEffect(Unit) {
        availableUpdate = if (BuildConfig.FORCE_UPDATE_PROMPT) {
            AppUpdate(versionName = "۲.۱.۰ (پیش‌نمایش)", versionCode = BuildConfig.VERSION_CODE + 1)
        } else {
            UpdateChecker.check()
        }
    }

    // Setup RTL top-level scaffold wrapping
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        availableUpdate?.let { update ->
            UpdateAvailableBottomSheet(
                update = update,
                onDismiss = { availableUpdate = null },
                onUpdate = {
                    if (StoreIntents.openUpdate(context) && !update.isRequired) availableUpdate = null
                }
            )
        }
        if (selectedCategoryId != null) {
            // Drill down view (full screen category counters)
            DhikrCounterScreen(
                categoryId = selectedCategoryId!!,
                viewModel = viewModel
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = true,
                drawerContent = {
                    AppDrawerContent(
                        viewModel = viewModel,
                        currentTab = currentTab,
                        fontScale = fontScale,
                        onNavigate = { tab ->
                            if (tab == "quran_audio" && currentTab != tab) quranAudioBackTab = currentTab
                            viewModel.selectTab(tab)
                        },
                        onClose = { coroutineScope.launch { drawerState.close() } }
                    )
                }
            ) {
            Scaffold(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                topBar = {
                    AppTopBar(
                        viewModel = viewModel,
                        currentTab = currentTab,
                        signedIn = accountUser != null,
                        fontScale = fontScale,
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onOpenAchievements = {
                            achievementsBackTab = "home"
                            viewModel.selectTab("achievements")
                        }
                    )
                },
                bottomBar = {
                    AppBottomBar(
                        currentTab = currentTab,
                        fontScale = fontScale,
                        onSelect = viewModel::selectTab
                    )
                }
            ) { innerPadding ->
                AppDestinations(
                    currentTab = currentTab,
                    viewModel = viewModel,
                    innerPadding = innerPadding,
                    openQuranPage = openQuranPage,
                    onQuranPageConsumed = onQuranPageConsumed,
                    onOpenShortcut = { destination ->
                        if (destination == "quran_audio") quranAudioBackTab = "home"
                        viewModel.selectTab(destination)
                    },
                    onAchievementsBack = { viewModel.selectTab(achievementsBackTab) },
                    onOpenAchievementsFromAccount = {
                        achievementsBackTab = "account"
                        viewModel.selectTab("achievements")
                    },
                    onOpenQuranAudio = {
                        quranAudioBackTab = "quran"
                        viewModel.selectTab("quran_audio")
                    }
                )
            }
            }
        }
    }
}
