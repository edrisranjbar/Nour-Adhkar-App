package com.example
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.AppLanguage
import com.example.ui.language.text

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Explore
import com.example.ui.screens.QiblaScreen
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Close
import com.example.ui.language.LocalizedIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import com.example.ui.language.LocalizedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DailyChecklistScreen
import com.example.ui.screens.DhikrCounterScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasbihScreen
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.ArticlesScreen
import com.example.ui.screens.AdhkarCollectionsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SandDark
import com.example.ui.theme.SunGold
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.NightBlue
import com.example.ui.viewmodel.AdhkarViewModel
import com.example.updates.AppUpdate
import com.example.updates.UpdateChecker
import com.example.notifications.AdhkarNotificationManager
import com.example.widget.ChecklistWidgetProvider
import androidx.compose.material3.rememberDrawerState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: AdhkarViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[AdhkarViewModel::class.java]
    }

    private var notificationCategory by mutableStateOf<String?>(null)
    private var openChecklistFromWidget by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handled gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationCategory = intent.getStringExtra(AdhkarNotificationManager.EXTRA_OPEN_CATEGORY)
        openChecklistFromWidget = intent.getBooleanExtra(ChecklistWidgetProvider.EXTRA_OPEN_CHECKLIST, false)
        enableEdgeToEdge()

        // Proactively request Notification permissions on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
            val appLanguage by viewModel.appLanguage.collectAsState()
            com.example.ui.language.LanguageProvider(appLanguage) {
            MyApplicationTheme(darkTheme = darkModeEnabled) {
                AppMainScaffold(
                    viewModel = viewModel,
                    notificationCategory = notificationCategory,
                    onNotificationCategoryConsumed = { notificationCategory = null },
                    openChecklistFromWidget = openChecklistFromWidget,
                    onChecklistWidgetIntentConsumed = { openChecklistFromWidget = false }
                )
            }
            }
        }
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        val isVolumeUp = event.keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP
        val isVolumeDown = event.keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN
        if (isVolumeUp || isVolumeDown) {
            if (viewModel.shouldInterceptVolumeKey(isVolumeUp)) {
                if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    viewModel.onVolumeKeyPressed(isVolumeUp)
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onResume() {
        super.onResume()
        com.example.prayer.AdhanScheduler(this).reschedule()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationCategory = intent.getStringExtra(AdhkarNotificationManager.EXTRA_OPEN_CATEGORY)
        openChecklistFromWidget = intent.getBooleanExtra(ChecklistWidgetProvider.EXTRA_OPEN_CHECKLIST, false)
    }
}

@Composable
fun AppMainScaffold(
    viewModel: AdhkarViewModel,
    notificationCategory: String? = null,
    onNotificationCategoryConsumed: () -> Unit = {},
    openChecklistFromWidget: Boolean = false,
    onChecklistWidgetIntentConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    var availableUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(notificationCategory) {
        if (notificationCategory == "morning" || notificationCategory == "evening") {
            viewModel.selectCategory(notificationCategory)
            onNotificationCategoryConsumed()
        }
    }

    LaunchedEffect(openChecklistFromWidget) {
        if (openChecklistFromWidget) {
            viewModel.selectTab("checklist")
            onChecklistWidgetIntentConsumed()
        }
    }

    LaunchedEffect(Unit) {
        availableUpdate = if (BuildConfig.FORCE_UPDATE_PROMPT) {
            AppUpdate(versionName = "۱.۵.۱ (پیش‌نمایش)", versionCode = BuildConfig.VERSION_CODE + 1)
        } else {
            UpdateChecker.check()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDailyChecklist()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Setup RTL top-level scaffold wrapping
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        availableUpdate?.let { update ->
            UpdateAvailableBottomSheet(
                update = update,
                onDismiss = { availableUpdate = null },
                onUpdate = {
                    val bazaarIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("bazaar://details?id=ir.adhkar.app")
                    )
                    runCatching { context.startActivity(bazaarIntent) }.onFailure {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://cafebazaar.ir/app/ir.adhkar.app")
                            )
                        )
                    }
                    availableUpdate = null
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
                    ModalDrawerSheet(
                        modifier = Modifier.width(300.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 24.dp)
                        ) {
                            val drawerItems = listOf(
                                Triple("home", "خانه", Icons.Default.Home),
                                Triple("adhkar", "اذکار و ادعیه", Icons.Default.Article),
                                Triple("checklist", "چک‌لیست روزانه", Icons.Default.Checklist),
                                Triple("tasbih", "ذکرشمار", null),
                                Triple("qibla", "قبله‌نما", Icons.Default.Explore),
                                Triple("articles", "مقالات", Icons.Default.Article),
                                Triple("favorites", "علاقه‌مندی‌ها", Icons.Default.Favorite),
                                Triple("donation", "حمایت مالی", Icons.Default.VolunteerActivism),
                                Triple("share", "اشتراک‌گذاری برنامه", Icons.Default.Share),
                                Triple("settings", "تنظیمات", Icons.Default.Settings),
                                Triple("about", "درباره برنامه", Icons.Default.Info)
                            )
                            drawerItems.forEach { (tab, label, icon) ->
                                NavigationDrawerItem(
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = (14 * fontScale).sp,
                                            fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    selected = tab !in setOf("share", "donation") && currentTab == tab,
                                    icon = {
                                        if (tab == "tasbih") {
                                            TasbihIcon(
                                                modifier = Modifier.size(24.dp),
                                                color = if (currentTab == tab) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        } else {
                                            Icon(icon ?: Icons.Default.Home, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        if (tab == "donation") {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://edrisranjbar.ir/donation")
                                                )
                                            )
                                        } else if (tab == "share") {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    if (language == AppLanguage.ARABIC) "أذكار نور؛ رفيقك اليومي للذكر والدعاء والتذكير بالأعمال اليومية\nhttps://cafebazaar.ir/app/ir.adhkar.app"
                                                    else "اذکار نور؛ همراه روزانه ذکر و نیایش، یادآوری اذکار و اعمال روزانه\nhttps://cafebazaar.ir/app/ir.adhkar.app"
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, language.text("اشتراک‌گذاری اذکار نور")))
                                        } else {
                                            viewModel.selectTab(tab)
                                        }
                                        coroutineScope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "نسخه ${BuildConfig.VERSION_NAME}",
                                fontSize = (10 * fontScale).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            ) {
            Scaffold(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "باز کردن منو",
                                tint = NightBlue
                            )
                        }
                        Image(
                            painter = painterResource(R.drawable.ic_nour_adhkar_logo),
                            contentDescription = language.text("نشان اذکار نور"),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(11.dp))
                        )
                        Text(
                            text = when (currentTab) {
                                "checklist" -> "چک‌لیست روزانه"
                                "tasbih" -> "ذکرشمار"
                                "qibla" -> "قبله‌نما"
                                "settings" -> "تنظیمات"
                                "about" -> "درباره برنامه"
                                "articles" -> "مقالات"
                                "adhkar" -> "اذکار و ادعیه"
                                "favorites" -> "علاقه‌مندی‌ها"
                                else -> "اذکار نور"
                            },
                            fontSize = (18 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = NightBlue,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                },
                bottomBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(74.dp),
                            shape = RoundedCornerShape(37.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, SoftBorder.copy(alpha = 0.8f)),
                            tonalElevation = 0.dp, // Disable tonal elevation to prevent dark tint overlays
                            shadowElevation = 10.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Home Tab
                                val isHomeSelected = currentTab == "home"
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { viewModel.selectTab("home") }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "خانه",
                                        tint = if (isHomeSelected) SunGold else NightBlue.copy(alpha = 0.75f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "خانه",
                                        fontSize = (10 * fontScale).sp,
                                        fontWeight = if (isHomeSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isHomeSelected) SunGold else NightBlue.copy(alpha = 0.75f)
                                    )
                                }

                                // 2. Adhkar Tab (right side in the RTL bottom bar)
                                val isAdhkarSelected = currentTab == "adhkar"
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { viewModel.selectTab("adhkar") }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Article,
                                        contentDescription = "اذکار و ادعیه",
                                        tint = if (isAdhkarSelected) SunGold else NightBlue.copy(alpha = 0.75f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "اذکار",
                                        fontSize = (10 * fontScale).sp,
                                        fontWeight = if (isAdhkarSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isAdhkarSelected) SunGold else NightBlue.copy(alpha = 0.75f)
                                    )
                                }

                                // 3. Tasbih Tab (Center Gradient Circular Button)
                                val isTasbihSelected = currentTab == "tasbih"
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .shadow(elevation = 8.dp, shape = CircleShape)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = if (isTasbihSelected) {
                                                    listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)) // Indigo-Purple gradient
                                                } else {
                                                    listOf(SunGold, SunGold.copy(alpha = 0.8f)) // Sage Green gradient
                                                }
                                            )
                                        )
                                        .clickable { viewModel.selectTab("tasbih") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    TasbihIcon(
                                        modifier = Modifier.size(28.dp),
                                        color = Color.White
                                    )
                                }

                                // 4. Daily Checklist Tab
                                val isChecklistSelected = currentTab == "checklist"
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { viewModel.selectTab("checklist") }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Checklist,
                                        contentDescription = "چک‌لیست",
                                        tint = if (isChecklistSelected) SunGold else NightBlue.copy(alpha = 0.75f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "چک‌لیست",
                                        fontSize = (10 * fontScale).sp,
                                        fontWeight = if (isChecklistSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChecklistSelected) SunGold else NightBlue.copy(alpha = 0.75f)
                                    )
                                }

                                // 5. Settings Tab (left side in the RTL bottom bar)
                                val isSettingsSelected = currentTab == "settings"
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { viewModel.selectTab("settings") }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "تنظیمات",
                                        tint = if (isSettingsSelected) SunGold else NightBlue.copy(alpha = 0.75f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "تنظیمات",
                                        fontSize = (10 * fontScale).sp,
                                        fontWeight = if (isSettingsSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSettingsSelected) SunGold else NightBlue.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                // Animate switching between the primary bottom-tabs
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "tabTransitions"
                ) { targetTab ->
                    when (targetTab) {
                        "home" -> HomeScreen(viewModel = viewModel, innerPadding = innerPadding)
                        "checklist" -> DailyChecklistScreen(viewModel = viewModel, innerPadding = innerPadding)
                        "tasbih" -> TasbihScreen(viewModel = viewModel, innerPadding = innerPadding)
                        "about" -> AboutScreen(viewModel = viewModel, innerPadding = innerPadding)
                        "articles" -> ArticlesScreen(viewModel = viewModel, innerPadding = innerPadding)
                        "adhkar" -> AdhkarCollectionsScreen(viewModel = viewModel, innerPadding = innerPadding)
                        "qibla" -> QiblaScreen(viewModel = viewModel, innerPadding = innerPadding)
                        "favorites" -> FavoritesScreen(viewModel = viewModel, innerPadding = innerPadding)
                        "settings" -> SettingsScreen(viewModel = viewModel, innerPadding = innerPadding)
                        else -> HomeScreen(viewModel = viewModel, innerPadding = innerPadding)
                    }
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateAvailableBottomSheet(
    update: AppUpdate,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color(0xFF071321).copy(alpha = 0.72f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xF2FFFFFF),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
            shadowElevation = 24.dp
        ) {
            Box(
                modifier = Modifier.background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFF8FBFF).copy(alpha = 0.96f),
                            Color(0xFFE7F0FF).copy(alpha = 0.92f),
                            Color(0xFFFFF5D9).copy(alpha = 0.90f)
                        )
                    )
                )
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color(0xFF10243B).copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = Color(0xFF10243B)
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(12.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1677FF), Color(0xFF5B45E8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SystemUpdateAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "نسخه جدید آماده است",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF071B31)
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "نسخه ${update.versionName} را از کافه‌بازار دریافت کنید و از تازه‌ترین بهبودها بهره ببرید.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF29445F),
                        lineHeight = 23.sp
                    )
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = onUpdate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.SystemUpdateAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("دریافت به‌روزرسانی", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFF23405D).copy(alpha = 0.35f))
                    ) {
                        Text("بعداً یادآوری کن", color = Color(0xFF18324D), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun TasbihIcon(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val beadRadius = 2.0f.dp.toPx()
        val centerOffset = Offset(size.width / 2, size.height / 2 - 2.5f.dp.toPx())
        val loopRadius = size.width / 3.4f

        repeat(10) { index ->
            val angle = (2 * Math.PI * index / 10) - Math.PI / 2
            val beadCenter = Offset(
                (centerOffset.x + loopRadius * Math.cos(angle)).toFloat(),
                (centerOffset.y + loopRadius * Math.sin(angle)).toFloat()
            )
            drawCircle(color = color, radius = beadRadius, center = beadCenter)
        }

        val imamahCenter = Offset(centerOffset.x, centerOffset.y + loopRadius)
        drawCircle(color = color, radius = 3.2f.dp.toPx(), center = imamahCenter)

        val tasselStart = Offset(centerOffset.x, imamahCenter.y + 3.2f.dp.toPx())
        val tasselEnd = Offset(centerOffset.x, imamahCenter.y + 8.5f.dp.toPx())
        drawLine(
            color = color.copy(alpha = 0.85f),
            start = tasselStart,
            end = tasselEnd,
            strokeWidth = 1.6f.dp.toPx()
        )
        drawCircle(color = color, radius = 1.5f.dp.toPx(), center = tasselEnd)
    }
}

// Simple tween container helper
private fun <T> tween(duration: Int) = androidx.compose.animation.core.tween<T>(duration)
