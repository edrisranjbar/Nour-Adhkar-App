package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notifications.AdhkarNotificationManager
import com.example.ui.AppMainScaffold
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AdhkarViewModel
import com.example.widget.ChecklistWidgetProvider
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var notificationCategory by mutableStateOf<String?>(null)
    private var openChecklistFromWidget by mutableStateOf(false)
    private var openPrayersFromWidget by mutableStateOf(false)
    private var openQuranPage by mutableStateOf<Int?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handled gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationCategory = intent.getStringExtra(AdhkarNotificationManager.EXTRA_OPEN_CATEGORY)
        openChecklistFromWidget = intent.getBooleanExtra(ChecklistWidgetProvider.EXTRA_OPEN_CHECKLIST, false)
        openPrayersFromWidget = intent.getBooleanExtra(com.example.widget.PrayerTimesWidgetProvider.EXTRA_OPEN_PRAYERS, false)
        openQuranPage = intent.getIntExtra(AdhkarNotificationManager.EXTRA_OPEN_QURAN_PAGE, 0).takeIf { it in 1..604 }
        enableEdgeToEdge()

        setContent {
            val viewModel: AdhkarViewModel = viewModel()
            LaunchedEffect(openPrayersFromWidget) {
                if (openPrayersFromWidget) {
                    viewModel.openPrayerSettings()
                    openPrayersFromWidget = false
                }
            }
            val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
            val appLanguage by viewModel.appLanguage.collectAsState()
            val onboardingComplete by viewModel.onboardingComplete.collectAsState()
            com.example.ui.language.LanguageProvider(appLanguage) {
            MyApplicationTheme(darkTheme = darkModeEnabled) {
                if (onboardingComplete) {
                    AppMainScaffold(
                        viewModel = viewModel,
                        onExitRequested = this@MainActivity::finish,
                        notificationCategory = notificationCategory,
                        onNotificationCategoryConsumed = { notificationCategory = null },
                        openChecklistFromWidget = openChecklistFromWidget,
                        onChecklistWidgetIntentConsumed = { openChecklistFromWidget = false },
                        openQuranPage = openQuranPage,
                        onQuranPageConsumed = { openQuranPage = null }
                    )
                } else {
                    OnboardingScreen(
                        language = appLanguage,
                        notificationsEnabled = viewModel.notificationsEnabled.collectAsState().value,
                        darkModeEnabled = darkModeEnabled,
                        onLanguageChange = viewModel::setAppLanguage,
                        onNotificationsChange = viewModel::setNotificationsEnabled,
                        onDarkModeChange = viewModel::setDarkModeEnabled,
                        onComplete = {
                            viewModel.completeOnboarding()
                            if (
                                viewModel.notificationsEnabled.value &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.data.repository.ProgressSyncRepository.onForeground(this, true)
        com.example.prayer.AdhanScheduler(this).reschedule()
        com.example.widget.PrayerTimesWidgetProvider.updateAll(this)
    }

    override fun onPause() {
        com.example.data.repository.ProgressSyncRepository.onForeground(this, false)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationCategory = intent.getStringExtra(AdhkarNotificationManager.EXTRA_OPEN_CATEGORY)
        openChecklistFromWidget = intent.getBooleanExtra(ChecklistWidgetProvider.EXTRA_OPEN_CHECKLIST, false)
        openPrayersFromWidget = intent.getBooleanExtra(com.example.widget.PrayerTimesWidgetProvider.EXTRA_OPEN_PRAYERS, false)
        openQuranPage = intent.getIntExtra(AdhkarNotificationManager.EXTRA_OPEN_QURAN_PAGE, 0).takeIf { it in 1..604 }
    }
}
