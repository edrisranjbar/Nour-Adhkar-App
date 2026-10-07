package com.example.ui.screens
import com.example.share.AppLinks

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import com.example.ui.language.LocalizedIcon as Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.language.LocalizedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.repository.AppInboxApi
import com.example.ui.util.toPersianDigits
import com.example.ui.language.AppLanguage
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var feedbackOpen by remember { mutableStateOf(false) }
    // Check for new team replies to the user's feedback (signed-in users only; failures are silent).
    val newReplies by AppInboxApi.newReplies.collectAsState()
    val aboutContext = LocalContext.current
    LaunchedEffect(Unit) {
        if (com.example.data.repository.AccountRepository.token(aboutContext) != null) runCatching { AppInboxApi.myFeedback(aboutContext) }
    }
    if (feedbackOpen) FeedbackSheet(
        onDismiss = { feedbackOpen = false },
        onSignIn = { feedbackOpen = false; viewModel.selectTab("account") }
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            // The app bar already shows «درباره برنامه»; no duplicate body title.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp)
            ) {
                // App Logo & Core About Text Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🕌", fontSize = 36.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "پروژه متن‌باز اذکار نور",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = (19 * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SunGold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                        text = "نسخه " + com.example.BuildConfig.VERSION_NAME.map {
                            if (it in '0'..'9') ('۰'.code + (it - '0')).toChar() else it
                        }.joinToString(""),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = (12 * fontScale).sp,
                                    color = NightBlue.copy(alpha = 0.6f)
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Beautifully written description directly from adhkar.ir/about concepts
                            Text(
                                text = if (com.example.ui.language.LocalAppLanguage.current == com.example.ui.language.AppLanguage.ARABIC)
                                    "أذكار نور مشروع مفتوح المصدر وغير ربحي يهدف إلى تيسير قراءة الأدعية والأذكار والتسبيح للمسلمين حول العالم.\n\nنؤمن بأن ذكر الله ينبغي أن يكون متاحًا للجميع في بيئة بسيطة وجميلة، بعيدًا عن الأهداف التجارية. جميع أقسام التطبيق مجانية بالكامل، بلا إعلانات أو تتبع، ومحتواه الأساسي متاح دون إنترنت.\n\nتسجيلات الأذكار بصوت مشاري راشد العفاسي من Makkah Live وInternet Archive، وتلاوات القرآن تُبث من mp3quran.net، والتلاوة آية بآية من EveryAyah.com. نص القرآن من Tanzil Project (tanzil.net)، والتفسير الميسر من مجمع الملك فهد لطباعة المصحف الشريف، والترجمتان الفارسيتان لفريق IslamHouse.com (QuranEnc) و«تفسير نور» للدكتور مصطفى خرمدل. وتفاسير السعدي والمختصر وابن كثير والطبري والقرطبي والبغوي والجلالين من Quran.com وQUL (Tarteel)."
                                else if (com.example.ui.language.LocalAppLanguage.current == com.example.ui.language.AppLanguage.URDU)
                                    "اذکار نور ایک اوپن سورس اور غیر منافع بخش منصوبہ ہے جس کا مقصد دنیا بھر کے مسلمانوں کے لیے دعاؤں، روزانہ اذکار اور تسبیحات کی ادائیگی کو آسان بنانا ہے۔\n\nہمارا یقین ہے کہ اللہ کا ذکر ایک سادہ، خوبصورت اور تجارتی مقاصد سے پاک ماحول میں سب کے لیے دستیاب ہونا چاہیے۔ ایپ کے تمام حصے مکمل طور پر مفت ہیں، اس میں کوئی اشتہار یا ٹریکنگ نہیں، اور اس کا بنیادی مواد انٹرنیٹ کے بغیر دستیاب ہے۔\n\nاذکار کی آڈیو مشاری راشد العفاسی کی آواز میں Makkah Live اور Internet Archive سے لی گئی ہے۔ قرآن کی تلاوتیں mp3quran.net سے اور آیت بہ آیت تلاوت EveryAyah.com سے چلتی ہے۔ قرآن کا متن Tanzil Project (tanzil.net) سے ہے۔ اردو ترجمہ مولانا محمد ابراہیم جوناگڑھی کا ہے (QuranEnc.com)، اور عربی تفاسیر — التفسیر المیسر (مجمع ملک فہد)، السعدی، المختصر، ابن کثیر، طبری، قرطبی، بغوی اور جلالین — Quran.com اور QUL (Tarteel) سے لی گئی ہیں۔"
                                else "پروژه اذکار یک تلاش متن‌باز، عام‌المنفعه و غیرانتفاعی است که با هدف تسهیل قرائت ادعیه، اذکار روزانه و تسبیحات برای مسلمانان سراسر جهان شکل گرفته است.\n\n" +
                                        "ما معتقدیم یاد و ذکر پروردگار باید در بستری زلال، ساده، زیبا و به دور از هرگونه هیاهو یا اهداف تجاری در دسترس همگان باشد. از این رو، تمام بخش‌های این نرم‌افزار به صورت کاملاً رایگان ارائه شده، فاقد هرگونه تبلیغ یا ردیابی است و محتوای اصلی آن بدون اینترنت در دسترس می‌ماند تا آرامش خاطر شما حفظ شود.\n\n" +
                                        "فایل‌های صوتی اذکار با صدای مشاری راشد العفاسی از Makkah Live و Internet Archive تهیه شده‌اند. تلاوت‌های قرآن از mp3quran.net و تلاوت آیه‌به‌آیه از EveryAyah.com پخش می‌شوند. متن قرآن از Tanzil Project (tanzil.net) است. ترجمه‌های فارسی آیات از گروه ترجمهٔ اسلام‌هاوس (QuranEnc.com) و «تفسیر نور» دکتر مصطفی خرمدل، و تفسیر عربی «التفسیر المیسر» از مجمع ملک فهد است. تفاسیر السعدی، المختصر، ابن‌کثیر، طبری، قرطبی، بغوی و جلالین (و ترجمهٔ فارسی المختصر و السعدی) از Quran.com و QUL (Tarteel) گرفته شده‌اند.",
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

                item {
                    OutlinedButton(
                        onClick = { feedbackOpen = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ارسال نظر و پیشنهاد", fontWeight = FontWeight.Bold)
                        if (newReplies > 0) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiary) {
                                Text("${newReplies.toPersianDigits()} پاسخ تازه", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
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
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
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

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val shareText = if (appLanguage == AppLanguage.ARABIC) {
                                    "أذكار نور؛ رفيقك اليومي للذكر والدعاء والتذكير بالأعمال اليومية\n${AppLinks.STORE_WEB_URL}"
                                } else if (appLanguage == AppLanguage.URDU) {
                                    "اذکار نور؛ ذکر و دعا کا روزانہ ساتھی، اذکار اور روزانہ اعمال کی یاد دہانی\n${AppLinks.STORE_WEB_URL}"
                                } else {
                                    "اذکار نور؛ همراه روزانه ذکر و نیایش، یادآوری اذکار و اعمال روزانه\n${AppLinks.STORE_WEB_URL}"
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, appLanguage.text("اشتراک‌گذاری اذکار نور")))
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, SoftBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "اشتراک‌گذاری برنامه",
                                fontSize = (14 * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                color = SandDark
                            )
                        }
                    }
                }

                // Contact, Telegram & Git Card (New Feature based on User Request)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                                        color = SandDark
                                    )
                                    Text(
                                        text = "edrisranjbar.dev@gmail.com",
                                        fontSize = 11.sp,
                                        color = NightBlue
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // GitHub Repository Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
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
                                        color = SandDark
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

private val FeedbackTypes = listOf("suggestion" to "پیشنهاد", "criticism" to "انتقاد", "other" to "سایر")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackSheet(onDismiss: () -> Unit, onSignIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val user by com.example.data.repository.AccountRepository.user.collectAsState()
    var type by remember { mutableStateOf(FeedbackTypes.first().first) }
    var typeMenuOpen by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    // 0 = «ارسال پیام», 1 = «پیشنهادهای من»; opens on «پیشنهادهای من» when a new reply is waiting.
    val newReplies by AppInboxApi.newReplies.collectAsState()
    var tab by remember { mutableStateOf(if (newReplies > 0) 1 else 0) }
    var sentCount by remember { mutableStateOf(0) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("ارسال نظر و پیشنهاد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val signedIn = user
            if (signedIn == null) {
                // Feedback is sent with the user's name, so signing in comes first.
                Text("برای ارسال نظر و پیشنهاد، ابتدا وارد حساب کاربری خود شوید تا بتوانیم پیام شما را پیگیری کنیم.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("ورود به حساب") }
                return@Column
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = tab == 0, onClick = { tab = 0 },
                    shape = AbsoluteRoundedCornerShape(topRight = 24.dp, bottomRight = 24.dp)
                ) {
                    Text("ارسال پیام")
                }
                SegmentedButton(
                    selected = tab == 1, onClick = { tab = 1 },
                    shape = AbsoluteRoundedCornerShape(topLeft = 24.dp, bottomLeft = 24.dp)
                ) {
                    Text(if (newReplies > 0 && tab != 1) "پیشنهادهای من (${newReplies.toPersianDigits()})" else "پیشنهادهای من")
                }
            }
            if (tab == 1) {
                MyFeedbackList(typeLabels = FeedbackTypes.toMap(), refreshKey = sentCount)
                return@Column
            }
            Text("ارسال با نام ${signedIn.name.ifBlank { signedIn.email }}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ExposedDropdownMenuBox(expanded = typeMenuOpen, onExpandedChange = { typeMenuOpen = it }) {
                OutlinedTextField(
                    value = FeedbackTypes.first { it.first == type }.second,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("نوع پیام") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuOpen) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                    FeedbackTypes.forEach { (value, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { type = value; typeMenuOpen = false })
                    }
                }
            }
            OutlinedTextField(
                value = message,
                onValueChange = { if (it.length <= 3000) message = it },
                label = { Text("پیام شما") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8
            )
            Button(
                enabled = !sending && message.trim().length >= 3,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                onClick = {
                    sending = true
                    status = ""
                    scope.launch {
                        try {
                            AppInboxApi.sendFeedback(context, type, message.trim())
                            message = ""
                            sentCount++
                            status = "پیام شما ارسال شد. سپاسگزاریم. پاسخ ما در «پیشنهادهای من» نمایش داده می‌شود."
                        } catch (e: com.example.data.repository.ServerException) {
                            status = if (e.code == 401) "نشست شما منقضی شده است. لطفاً دوباره وارد حساب شوید."
                            else "ارسال انجام نشد. لطفاً بعداً دوباره تلاش کنید."
                        } catch (e: Exception) {
                            status = if (e is java.io.IOException) "ارسال انجام نشد. اتصال اینترنت را بررسی کنید."
                            else "ارسال انجام نشد. لطفاً بعداً دوباره تلاش کنید."
                        } finally { sending = false }
                    }
                }
            ) { Text(if (sending) "در حال ارسال…" else "ارسال پیام") }
            if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}
