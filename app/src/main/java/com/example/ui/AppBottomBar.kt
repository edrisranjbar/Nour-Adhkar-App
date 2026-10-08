package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TasbihIcon
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text
import com.example.ui.language.text
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold

@Composable
fun AppBottomBar(
    currentTab: String,
    fontScale: Float,
    onSelect: (String) -> Unit
) {
    if (currentTab != "quran") {
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
                        .clickable { onSelect("home") }
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

                // 2. Quran Tab (right side in the RTL bottom bar)
                val isQuranSelected = currentTab in setOf("quran", "quran_audio")
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect("quran") }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "قرآن کریم",
                        tint = if (isQuranSelected) SunGold else NightBlue.copy(alpha = 0.75f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "قرآن",
                        fontSize = (10 * fontScale).sp,
                        fontWeight = if (isQuranSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isQuranSelected) SunGold else NightBlue.copy(alpha = 0.75f)
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
                        .clickable { onSelect("tasbih") },
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
                        .clickable { onSelect("checklist") }
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
                        .clickable { onSelect("settings") }
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
}
