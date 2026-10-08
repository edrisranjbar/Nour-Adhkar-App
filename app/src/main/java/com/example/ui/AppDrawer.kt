package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.BuildConfig
import com.example.ui.components.TasbihIcon
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text
import com.example.ui.language.text
import com.example.ui.viewmodel.AdhkarViewModel

@Composable
fun AppDrawerContent(
    viewModel: AdhkarViewModel,
    currentTab: String,
    fontScale: Float,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        com.example.ui.screens.DrawerProfileHeader(
            streak = com.example.ui.screens.rememberCurrentStreak(viewModel),
            onClick = {
                viewModel.selectTab("account")
                onClose()
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val drawerItems = listOf(
                Triple("adhkar", "اذکار و ادعیه", Icons.Default.Article),
                Triple("calendar", "تقویم", Icons.Default.CalendarMonth),
                Triple("quran_audio", "قرآن صوتی", Icons.Default.Headphones),
                Triple("qibla", "قبله‌نما", Icons.Default.Explore),
                Triple("scholars", "علما و مشاهیر", Icons.Default.RecordVoiceOver),
                Triple("articles", "مقالات", Icons.Default.Article),
                Triple("qaza", "قضای روزه", Icons.Default.EventRepeat),
                Triple("donation", "حمایت مالی", Icons.Default.VolunteerActivism),
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
                    selected = tab != "donation" && currentTab == tab,
                    icon = {
                        if (tab == "tasbih") {
                            TasbihIcon(
                                modifier = Modifier.size(24.dp),
                                color = if (currentTab == tab) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
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
                        } else {
                            onNavigate(tab)
                        }
                        onClose()
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
