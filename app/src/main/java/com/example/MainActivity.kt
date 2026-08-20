package com.example

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.screens.FavoritesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SandDark
import com.example.ui.theme.SunGold
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.NightBlue
import com.example.ui.viewmodel.AdhkarViewModel
import com.example.updates.AppUpdate
import com.example.updates.UpdateChecker
import androidx.compose.material3.rememberDrawerState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handled gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            val viewModel: AdhkarViewModel = viewModel()
            val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
            MyApplicationTheme(darkTheme = darkModeEnabled) {
                AppMainScaffold(viewModel)
            }
        }
    }
}

@Composable
fun AppMainScaffold(viewModel: AdhkarViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    var availableUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        availableUpdate = UpdateChecker.check()
    }

    availableUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = { availableUpdate = null },
            title = { Text("نسخه جدید در دسترس است") },
            text = { Text("نسخه ${update.versionName} از کافه‌بازار قابل دریافت است.") },
            confirmButton = {
                Button(onClick = {
                    val bazaarIntent = Intent(Intent.ACTION_VIEW, Uri.parse("bazaar://details?id=ir.adhkar.app"))
                    runCatching { context.startActivity(bazaarIntent) }.onFailure {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cafebazaar.ir/app/ir.adhkar.app")))
                    }
                    availableUpdate = null
                }) { Text("به‌روزرسانی") }
            },
            dismissButton = {
                OutlinedButton(onClick = { availableUpdate = null }) { Text("بعداً") }
            }
        )
    }

    // Setup RTL top-level scaffold wrapping
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp)
                        ) {
                            Text(
                                text = "نور اذکار",
                                fontSize = (22 * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                color = NightBlue
                            )
                            Text(
                                text = "همراه روزانه ذکر و نیایش",
                                fontSize = (12 * fontScale).sp,
                                color = NightBlue.copy(alpha = 0.65f),
                                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                            )

                            val drawerItems = listOf(
                                Triple("home", "خانه", Icons.Default.Home),
                                Triple("checklist", "چک‌لیست روزانه", Icons.Default.Checklist),
                                Triple("tasbih", "تسبیح‌شمار", null),
                                Triple("articles", "مقالات", Icons.Default.Article),
                                Triple("favorites", "علاقه‌مندی‌ها", Icons.Default.Favorite),
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
                                    selected = tab != "share" && currentTab == tab,
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
                                        if (tab == "share") {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "اذکار نور؛ همراه روزانه ذکر و نیایش، یادآوری اذکار و اعمال روزانه\nhttps://cafebazaar.ir/app/ir.adhkar.app"
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری نور اذکار"))
                                        } else {
                                            viewModel.selectTab(tab)
                                        }
                                        coroutineScope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
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
                        Text(
                            text = when (currentTab) {
                                "checklist" -> "چک‌لیست روزانه"
                                "tasbih" -> "تسبیح‌شمار"
                                "settings" -> "تنظیمات"
                                "about" -> "درباره برنامه"
                                "articles" -> "مقالات"
                                "favorites" -> "علاقه‌مندی‌ها"
                                else -> "نور اذکار"
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

                                // 2. Tasbih Tab (Center Gradient Circular Button)
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

                                // 3. Daily Checklist Tab
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
