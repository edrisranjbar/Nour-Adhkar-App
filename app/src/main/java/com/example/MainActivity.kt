package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ArticlesScreen
import com.example.ui.screens.DhikrCounterScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasbihScreen
import com.example.ui.screens.AboutScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SandDark
import com.example.ui.theme.SunGold
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.NightBlue
import com.example.ui.viewmodel.AdhkarViewModel

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
            MyApplicationTheme {
                val viewModel: AdhkarViewModel = viewModel()
                AppMainScaffold(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppMainScaffold(viewModel: AdhkarViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()

    // Setup RTL top-level scaffold wrapping
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (selectedCategoryId != null) {
            // Drill down view (full screen category counters)
            DhikrCounterScreen(
                categoryId = selectedCategoryId!!,
                viewModel = viewModel
            )
        } else {
            // Main Bottom Tab Scaffold View
            Scaffold(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
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
                            color = Color(0xF2E4EFE0), // Highly opaque light-sage frosted background
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

                                // 2. Articles Tab
                                val isArticlesSelected = currentTab == "articles"
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { viewModel.selectTab("articles") }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "مقالات",
                                        tint = if (isArticlesSelected) SunGold else NightBlue.copy(alpha = 0.75f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "مقالات",
                                        fontSize = (10 * fontScale).sp,
                                        fontWeight = if (isArticlesSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isArticlesSelected) SunGold else NightBlue.copy(alpha = 0.75f)
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
                                    // Custom-drawn resolution-independent Tasbih (prayer beads) icon
                                    Canvas(modifier = Modifier.size(28.dp)) {
                                        val beadRadius = 2.0f.dp.toPx()
                                        val centerOffset = Offset(size.width / 2, size.height / 2 - 2.5f.dp.toPx())
                                        val loopRadius = size.width / 3.4f
                                        
                                        // Draw the circular loop of 10 beads
                                        val numBeads = 10
                                        for (i in 0 until numBeads) {
                                            val angle = (2 * Math.PI * i / numBeads) - Math.PI / 2
                                            val beadCenter = Offset(
                                                (centerOffset.x + loopRadius * Math.cos(angle)).toFloat(),
                                                (centerOffset.y + loopRadius * Math.sin(angle)).toFloat()
                                            )
                                            drawCircle(
                                                color = Color.White,
                                                radius = beadRadius,
                                                center = beadCenter
                                            )
                                        }
                                        
                                        // Draw the leader bead (Imamah) at the bottom
                                        val imamahCenter = Offset(centerOffset.x, centerOffset.y + loopRadius)
                                        drawCircle(
                                            color = Color.White,
                                            radius = 3.2f.dp.toPx(),
                                            center = imamahCenter
                                        )
                                        
                                        // Draw the hanging tassel
                                        val tasselStart = Offset(centerOffset.x, imamahCenter.y + 3.2f.dp.toPx())
                                        val tasselEnd = Offset(centerOffset.x, imamahCenter.y + 8.5f.dp.toPx())
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.85f),
                                            start = tasselStart,
                                            end = tasselEnd,
                                            strokeWidth = 1.6f.dp.toPx()
                                        )
                                        
                                        // Tassel bead tip
                                        drawCircle(
                                            color = Color.White,
                                            radius = 1.5f.dp.toPx(),
                                            center = tasselEnd
                                        )
                                    }
                                }

                                // 4. About Tab
                                val isAboutSelected = currentTab == "about"
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { viewModel.selectTab("about") }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "درباره",
                                        tint = if (isAboutSelected) SunGold else NightBlue.copy(alpha = 0.75f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "درباره",
                                        fontSize = (10 * fontScale).sp,
                                        fontWeight = if (isAboutSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isAboutSelected) SunGold else NightBlue.copy(alpha = 0.75f)
                                    )
                                }

                                // 5. Settings Tab
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
                        fadeIn(animationSpec = tween(220)) with fadeOut(animationSpec = tween(220))
                    },
                    label = "tabTransitions"
                ) { targetTab ->
                    when (targetTab) {
                        "home" -> HomeScreen(viewModel = viewModel, innerPadding = innerPadding)
                        "articles" -> ArticlesScreen(viewModel = viewModel, innerPadding = innerPadding)
                        "tasbih" -> TasbihScreen(viewModel = viewModel, innerPadding = innerPadding)
                        "about" -> AboutScreen(viewModel = viewModel, innerPadding = innerPadding)
                        "settings" -> SettingsScreen(viewModel = viewModel, innerPadding = innerPadding)
                        else -> HomeScreen(viewModel = viewModel, innerPadding = innerPadding)
                    }
                }
            }
        }
    }
}

// Simple tween container helper
private fun <T> tween(duration: Int) = androidx.compose.animation.core.tween<T>(duration)
