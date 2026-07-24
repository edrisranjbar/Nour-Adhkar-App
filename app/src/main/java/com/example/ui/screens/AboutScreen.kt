package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SandDark
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.theme.TextPersian
import com.example.ui.viewmodel.AdhkarViewModel

@Composable
fun AboutScreen(
    viewModel: AdhkarViewModel,
    innerPadding: PaddingValues
) {
    val fontScale by viewModel.fontScale.collectAsState()
    val uriHandler = LocalUriHandler.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            // Screen Header
            Text(
                text = "درباره برنامه",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = (24 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = SandDark
                ),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp)
            ) {
                // App Logo & Core About Text Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SoftBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF7FAF3)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🕌", fontSize = 36.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "پروژه متن‌باز اذکار (Adhkar.ir)",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = (19 * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SunGold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "نسخه ۱.۰.۰",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = (12 * fontScale).sp,
                                    color = NightBlue.copy(alpha = 0.6f)
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Beautifully written description directly from adhkar.ir/about concepts
                            Text(
                                text = "پروژه اذکار یک تلاش متن‌باز، عام‌المنفعه و غیرانتفاعی است که با هدف تسهیل قرائت ادعیه، اذکار روزانه و تسبیحات برای مسلمانان سراسر جهان شکل گرفته است.\n\n" +
                                        "ما معتقدیم یاد و ذکر پروردگار باید در بستری زلال، ساده، زیبا و به دور از هرگونه هیاهو یا اهداف تجاری در دسترس همگان باشد. از این رو، تمام بخش‌های این نرم‌افزار به صورت کاملاً رایگان ارائه شده، فاقد هرگونه تبلیغ یا ردیابی است و به صورت کاملاً آفلاین کار می‌کند تا آرامش خاطر شما حفظ شود.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = (13.5 * fontScale).sp,
                                    color = TextPersian,
                                    lineHeight = 22.sp
                                ),
                                textAlign = TextAlign.Justify
                            )
                        }
                    }
                }

                // Donation banner
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri("https://edrisranjbar.ir/donation") },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)),
                        border = BorderStroke(1.dp, Color(0xFFE8C978))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFFFE7A8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFC78600),
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "حمایت از توسعه اذکار نور",
                                    fontSize = (14 * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SandDark
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "برای ادامه توسعه رایگان و بدون تبلیغ برنامه",
                                    fontSize = (11.5 * fontScale).sp,
                                    color = NightBlue
                                )
                            }
                        }
                    }
                }

                // Contact, Telegram & Git Card (New Feature based on User Request)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SoftBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "ارتباط با ما و مشارکت",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = (15 * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SandDark
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "این برنامه داوطلبانه توسعه یافته است. شما می‌توانید جهت ارسال پیشنهادات، گزارش خطاها و یا مشارکت در بهبود کدهای برنامه از راه‌های زیر با ما در ارتباط باشید:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = (12.5 * fontScale).sp,
                                    color = TextPersian,
                                    lineHeight = 18.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Email Address Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFECEFF1)) // Soft gray background
                                    .clickable { uriHandler.openUri("mailto:edrisranjbar.dev@gmail.com") }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "پست الکترونیکی",
                                    tint = Color(0xFF607D8B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "پست الکترونیکی",
                                        fontSize = (13 * fontScale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF37474F)
                                    )
                                    Text(
                                        text = "edrisranjbar.dev@gmail.com",
                                        fontSize = 11.sp,
                                        color = Color(0xFF455A64)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // GitHub Repository Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF1F8E9)) // Soft green/sage background
                                    .clickable { uriHandler.openUri("https://github.com/edrisranjbar/nour-adhkar") }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "گیت‌هاب",
                                    tint = Color(0xFF558B2F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "مخزن متن‌باز پروژه در گیت‌هاب",
                                        fontSize = (13 * fontScale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF33691E)
                                    )
                                    Text(
                                        text = "github.com/edrisranjbar/nour-adhkar",
                                        fontSize = 11.sp,
                                        color = Color(0xFF558B2F)
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}
