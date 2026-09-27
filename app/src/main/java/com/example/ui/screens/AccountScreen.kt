package com.example.ui.screens

import android.content.Context
import com.example.data.repository.AuthResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material3.TextButton
import androidx.compose.foundation.border
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AccountRepository
import com.example.data.repository.AccountUser
import com.example.data.repository.AuthException
import com.example.ui.theme.SunGold
import com.example.ui.util.toPersianDigits
import kotlinx.coroutines.launch

private val ProfileGreenDark = Color(0xFF0E4B38)
private val ProfileGreen = Color(0xFF2E6B4E)
private val ProfileGradient = Brush.linearGradient(listOf(ProfileGreenDark, ProfileGreen))

/** Profile: sign-in form or account details, with the achievements banner last. */
@Composable
fun AccountScreen(innerPadding: PaddingValues, onOpenAchievements: () -> Unit) {
    val context = LocalContext.current
    LaunchedInit(context)
    val user by AccountRepository.user.collectAsState()
    val scope = rememberCoroutineScope()
    val current = user
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (current != null) {
            AccountDetailsCard(current) { scope.launch { AccountRepository.logout(context) } }
        } else {
            SectionCard {
                Text("ورود یا ساخت حساب", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                AuthForm(onSignedIn = {})
            }
        }
        AchievementsEntryCard(onClick = onOpenAchievements)
    }
}

@Composable
private fun Avatar(user: AccountUser?, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        val initial = user?.let { (it.name.ifBlank { it.email }).trim().firstOrNull()?.uppercase() }
        if (initial != null) {
            Text(initial, color = Color.White, fontSize = (size * 0.44f).sp, fontWeight = FontWeight.Bold)
        } else {
            Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size((size * 0.56f).dp))
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun AccountDetailsCard(user: AccountUser, onLogout: () -> Unit) {
    SectionCard {
        Text("اطلاعات حساب", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (user.name.isNotBlank()) DetailRow(Icons.Rounded.Person, "نام", user.name)
        DetailRow(Icons.Rounded.Email, "ایمیل", user.email)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("خروج از حساب")
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(ProfileGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = ProfileGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Telegram-style drawer header: avatar, name and email (or a sign-in prompt); opens Profile. */
@Composable
fun DrawerProfileHeader(streak: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    LaunchedInit(context)
    val user by AccountRepository.user.collectAsState()
    val current = user
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = ProfileGreenDark) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(ProfileGradient)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Avatar(current, size = 64)
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "زنجیره ${streak.toPersianDigits()} روزه"
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(3.dp))
                    Text(
                        streak.toPersianDigits(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                current?.name?.ifBlank { null } ?: if (current == null) "ورود / ثبت‌نام" else current.email,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                current?.email ?: "مشاهده پروفایل و دستاوردها",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AchievementsEntryCard(onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = ProfileGreenDark) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = SunGold, modifier = Modifier.size(36.dp))
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "نشان‌ها و دستاوردها",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "پیشرفت و مراحل گشوده‌شده‌ات را ببین",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun LaunchedInit(context: Context) {
    remember { AccountRepository.init(context); true }
}

/** Email login/register. Used in onboarding and the profile screen. */
@Composable
fun AuthForm(onSignedIn: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedInit(context)
    val scope = rememberCoroutineScope()
    var registerMode by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingEmail by rememberSaveable { mutableStateOf<String?>(null) }
    var code by rememberSaveable { mutableStateOf("") }
    var resendIn by rememberSaveable { mutableStateOf(0) }
    var info by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(resendIn) {
        if (resendIn > 0) {
            kotlinx.coroutines.delay(1000)
            resendIn -= 1
        }
    }

    fun handle(result: AuthResult) {
        when (result) {
            is AuthResult.SignedIn -> onSignedIn()
            is AuthResult.VerificationRequired -> {
                pendingEmail = result.email
                code = ""
                resendIn = result.retryAfterSeconds
                info = null
            }
        }
    }

    fun launchAuth(block: suspend () -> Unit) {
        error = null
        loading = true
        scope.launch {
            try {
                block()
            } catch (e: AuthException) {
                error = e.message
            } catch (e: Exception) {
                error = "ورود انجام نشد. دوباره تلاش کنید."
            } finally {
                loading = false
            }
        }
    }

    fun submit() {
        when {
            registerMode && name.isBlank() -> error = "نام را وارد کنید."
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> error = "ایمیل معتبر نیست."
            password.length < 6 -> error = "رمز عبور باید حداقل ۶ کاراکتر باشد."
            loading -> Unit
            else -> launchAuth {
                handle(
                    if (registerMode) AccountRepository.register(context, name, email, password)
                    else AccountRepository.login(context, email, password)
                )
            }
        }
    }

    val verifying = pendingEmail
    if (verifying != null) {
        VerificationStep(
            email = verifying,
            code = code,
            onCodeChange = { value -> code = value.filter(Char::isDigit).take(5); error = null },
            loading = loading,
            error = error,
            info = info,
            resendIn = resendIn,
            onVerify = {
                if (code.length != 5) error = "کد ۵ رقمی را کامل وارد کنید."
                else launchAuth { handle(AccountRepository.verifyEmail(context, verifying, password, code)) }
            },
            onResend = {
                launchAuth {
                    resendIn = AccountRepository.resendCode(verifying)
                    info = "کد جدید ارسال شد."
                }
            },
            onChangeEmail = { pendingEmail = null; code = ""; error = null; info = null },
            modifier = modifier
        )
        return
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("ورود", "ثبت‌نام").forEachIndexed { index, label ->
                SegmentedButton(
                    selected = registerMode == (index == 1),
                    onClick = { registerMode = index == 1; error = null },
                    shape = SegmentedButtonDefaults.itemShape(index, 2)
                ) { Text(label) }
            }
        }
        if (registerMode) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("نام") }, singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("ایمیل") }, singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("رمز عبور") }, singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (showPassword) "پنهان کردن رمز" else "نمایش رمز"
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = ::submit,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ProfileGreen, contentColor = Color.White)
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            else Text(if (registerMode) "ساخت حساب" else "ورود", fontWeight = FontWeight.Bold)
        }
    }
}

/** Enter the 5-digit code emailed through Resend; supports resend with a countdown. */
@Composable
private fun VerificationStep(
    email: String,
    code: String,
    onCodeChange: (String) -> Unit,
    loading: Boolean,
    error: String?,
    info: String?,
    resendIn: Int,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    LaunchedEffect(code) { if (code.length == 5 && !loading) onVerify() }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(ProfileGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MarkEmailRead, contentDescription = null, tint = ProfileGreen, modifier = Modifier.size(28.dp))
        }
        Text("تأیید ایمیل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "کد ۵ رقمی ارسال‌شده به این ایمیل را وارد کنید:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(email, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

        // Five boxes drawn over one hidden field, so paste and SMS-style autofill both work.
        androidx.compose.foundation.text.BasicTextField(
            value = code,
            onValueChange = onCodeChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier
                .focusRequester(focusRequester)
                .semantics { contentDescription = "کد تأیید ۵ رقمی" },
            decorationBox = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(5) { index ->
                            val char = code.getOrNull(index)
                            val active = index == code.length
                            Box(
                                Modifier
                                    .size(width = 48.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(
                                        width = if (active) 2.dp else 1.dp,
                                        color = if (active) ProfileGreen else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    char?.toString() ?: "",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) }
        info?.let { Text(it, color = ProfileGreen, style = MaterialTheme.typography.bodySmall) }

        Button(
            onClick = onVerify,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ProfileGreen, contentColor = Color.White)
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            else Text("تأیید و ورود", fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onChangeEmail, enabled = !loading) { Text("تغییر ایمیل") }
            TextButton(onClick = onResend, enabled = !loading && resendIn == 0) {
                Text(if (resendIn > 0) "ارسال دوباره (${resendIn.toPersianDigits()})" else "ارسال دوباره کد")
            }
        }
    }
}
