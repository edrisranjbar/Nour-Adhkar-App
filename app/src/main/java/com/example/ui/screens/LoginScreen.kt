package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.R

private enum class LoginStep { Welcome, Email, Verify, ForgotPassword, ResetPassword }


/**
 * Signed-out profile: a welcome page with an illustration and two choices (Google or email).
 * Email opens a clean login/register form; unverified emails continue to the 5-digit code step.
 */
@Composable
fun LoginScreen(
    innerPadding: PaddingValues,
    onContinueWithoutAccount: () -> Unit
) {
    // Signing in flips AccountRepository.user, and AccountScreen then shows the account details.
    val auth = rememberAuthController(onSignedIn = {})
    var emailChosen by rememberSaveable { mutableStateOf(false) }
    val step = when {
        auth.resetEmail != null -> LoginStep.ResetPassword
        auth.forgotMode -> LoginStep.ForgotPassword
        auth.pendingEmail != null -> LoginStep.Verify
        emailChosen -> LoginStep.Email
        else -> LoginStep.Welcome
    }
    fun back() {
        when (step) {
            LoginStep.ResetPassword -> auth.startForgotPassword()
            LoginStep.ForgotPassword -> auth.cancelForgotPassword()
            LoginStep.Verify -> auth.changeEmail()
            LoginStep.Email -> { emailChosen = false; auth.error = null }
            LoginStep.Welcome -> Unit
        }
    }
    BackHandler(enabled = step != LoginStep.Welcome && !auth.loading) { back() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .imePadding()
    ) {
        val minHeight = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "loginStep"
            ) { current ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = minHeight)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    when (current) {
                        LoginStep.Welcome -> WelcomeStep(
                            auth = auth,
                            onChooseEmail = { auth.error = null; emailChosen = true },
                            onContinueWithoutAccount = onContinueWithoutAccount
                        )
                        LoginStep.Email -> {
                            BackRow(enabled = !auth.loading, onBack = ::back)
                            EmailStep(auth)
                        }
                        LoginStep.Verify -> {
                            BackRow(enabled = !auth.loading, onBack = ::back)
                            Spacer(Modifier.height(8.dp))
                            VerificationStep(auth)
                        }
                        LoginStep.ForgotPassword -> {
                            BackRow(enabled = !auth.loading, onBack = ::back)
                            ForgotPasswordStep(auth)
                        }
                        LoginStep.ResetPassword -> {
                            BackRow(enabled = !auth.loading, onBack = ::back)
                            ResetPasswordStep(auth)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.WelcomeStep(
    auth: AuthController,
    onChooseEmail: () -> Unit,
    onContinueWithoutAccount: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroIllustration()
        Spacer(Modifier.height(24.dp))
        Text(
            "به اذکار نور خوش آمدید",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (EmailSignInEnabled) "وارد حساب خود شوید یا در چند ثانیه حساب تازه‌ای بسازید. همه‌ی بخش‌های برنامه بدون حساب هم در دسترس‌اند."
            else "با حساب گوگل وارد شوید یا بدون حساب ادامه دهید. همه‌ی بخش‌های برنامه بدون حساب هم در دسترس‌اند.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }

    Spacer(Modifier.weight(1f).heightIn(min = 28.dp))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        auth.error?.let { ErrorBanner(it) }
        GoogleSignInButton(
            loading = auth.loading && auth.googlePending,
            enabled = !auth.loading,
            onClick = auth::signInWithGoogle
        )
        if (EmailSignInEnabled) {
            Button(
                onClick = onChooseEmail,
                enabled = !auth.loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ProfileGreen, contentColor = Color.White)
            ) {
                Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(12.dp))
                Text("ورود با ایمیل", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onContinueWithoutAccount,
                enabled = !auth.loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ProfileGreen, contentColor = Color.White)
            ) {
                Text("ادامه بدون حساب", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Green gradient panel with the night-scene illustration. */
@Composable
private fun HeroIllustration() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(ProfileGradient),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Soft decorative rings.
        Box(
            Modifier
                .size(220.dp)
                .align(Alignment.TopStart)
                .offset(x = (-70).dp, y = (-80).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Box(
            Modifier
                .size(160.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )
        // Night scene: crescent, stars, lantern and mosque (vector, drawn for this 300dp panel).
        Image(
            painter = painterResource(R.drawable.login_illustration),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun EmailStep(auth: AuthController) {
    val focusManager = LocalFocusManager.current
    val register = auth.registerMode
    val ltr = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
    val fieldShape = RoundedCornerShape(16.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ProfileGreen,
        focusedLabelColor = ProfileGreen,
        focusedLeadingIconColor = ProfileGreen,
        cursorColor = ProfileGreen
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(ProfileGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (register) Icons.Rounded.PersonAdd else Icons.Rounded.Email,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (register) "ساخت حساب تازه" else "ورود با ایمیل",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (register) "پس از ثبت‌نام، یک کد تأیید ۵ رقمی به ایمیل شما فرستاده می‌شود."
                else "خوش برگشتید! ایمیل و رمز عبورتان را وارد کنید.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(4.dp))

        AnimatedVisibility(visible = register, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            OutlinedTextField(
                value = auth.name,
                onValueChange = { auth.name = it; auth.error = null },
                label = { Text("نام") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
        }
        OutlinedTextField(
            value = auth.email,
            onValueChange = { auth.email = it; auth.error = null },
            label = { Text("ایمیل") },
            placeholder = { Text("name@example.com", style = ltr) },
            singleLine = true,
            textStyle = ltr,
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            shape = fieldShape,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = auth.password,
            onValueChange = { auth.password = it; auth.error = null },
            label = { Text("رمز عبور") },
            supportingText = if (register) {
                { Text("دست‌کم ۶ کاراکتر") }
            } else null,
            singleLine = true,
            textStyle = ltr,
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = { PasswordToggle(auth) },
            visualTransformation = if (auth.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); auth.submit() }),
            shape = fieldShape,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        if (!register) {
            TextButton(
                onClick = auth::startForgotPassword,
                enabled = !auth.loading,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("فراموشی رمز عبور؟", color = ProfileGreen)
            }
        }

        auth.error?.let { ErrorBanner(it) }

        Button(
            onClick = { focusManager.clearFocus(); auth.submit() },
            enabled = !auth.loading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ProfileGreen, contentColor = Color.White)
        ) {
            if (auth.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
            else Text(if (register) "ساخت حساب" else "ورود", fontWeight = FontWeight.Bold)
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (register) "قبلاً حساب ساخته‌اید؟" else "حساب کاربری ندارید؟",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { auth.switchMode(!register) }, enabled = !auth.loading) {
                Text(
                    if (register) "وارد شوید" else "ثبت‌نام کنید",
                    color = ProfileGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BackRow(enabled: Boolean, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack, enabled = enabled) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت")
        }
    }
}

@Composable
internal fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
