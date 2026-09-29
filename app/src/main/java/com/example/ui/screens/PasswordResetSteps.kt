package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.ui.util.toPersianDigits

/** Step 1 of password reset: enter the account email to receive a 5-digit code. */
@Composable
internal fun ForgotPasswordStep(auth: AuthController) {
    val focusManager = LocalFocusManager.current
    val ltr = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StepHeader(
            icon = Icons.Rounded.LockReset,
            title = "فراموشی رمز عبور",
            subtitle = "ایمیل حسابتان را وارد کنید تا یک کد ۵ رقمی برای تعیین رمز تازه برایتان بفرستیم."
        )
        OutlinedTextField(
            value = auth.email,
            onValueChange = { auth.email = it; auth.error = null },
            label = { Text("ایمیل") },
            placeholder = { Text("name@example.com", style = ltr) },
            singleLine = true,
            textStyle = ltr,
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { focusManager.clearFocus(); auth.sendResetCode() }),
            shape = RoundedCornerShape(16.dp),
            colors = resetFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        auth.error?.let { ErrorBanner(it) }
        PrimaryButton("ارسال کد بازیابی", auth.loading) { focusManager.clearFocus(); auth.sendResetCode() }
        TextButton(onClick = auth::cancelForgotPassword, enabled = !auth.loading, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("بازگشت به ورود", color = ProfileGreen, fontWeight = FontWeight.Bold)
        }
    }
}

/** Step 2 of password reset: the emailed code plus the new password; success signs in. */
@Composable
internal fun ResetPasswordStep(auth: AuthController) {
    val email = auth.resetEmail ?: return
    val focusManager = LocalFocusManager.current
    val codeFocus = remember { FocusRequester() }
    val ltr = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
    LaunchedEffect(Unit) { runCatching { codeFocus.requestFocus() } }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(
            icon = Icons.Rounded.Key,
            title = "تعیین رمز عبور تازه",
            subtitle = "کد ۵ رقمی ارسال‌شده به این ایمیل و رمز عبور تازه را وارد کنید:"
        )
        Text(email, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        CodeInput(auth.code, auth::onCodeChange, codeFocus, "کد بازیابی ۵ رقمی")
        OutlinedTextField(
            value = auth.newPassword,
            onValueChange = { auth.newPassword = it; auth.error = null },
            label = { Text("رمز عبور تازه") },
            supportingText = { Text("دست‌کم ۶ کاراکتر") },
            singleLine = true,
            textStyle = ltr,
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = { PasswordToggle(auth) },
            visualTransformation = if (auth.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); auth.confirmPasswordReset() }),
            shape = RoundedCornerShape(16.dp),
            colors = resetFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        auth.error?.let { ErrorBanner(it) }
        auth.info?.let { Text(it, color = ProfileGreen, style = MaterialTheme.typography.bodySmall) }
        PrimaryButton("ذخیره رمز و ورود", auth.loading) { focusManager.clearFocus(); auth.confirmPasswordReset() }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = auth::startForgotPassword, enabled = !auth.loading) { Text("تغییر ایمیل") }
            TextButton(onClick = auth::sendResetCode, enabled = !auth.loading && auth.resendIn == 0) {
                Text(if (auth.resendIn > 0) "ارسال دوباره (${auth.resendIn.toPersianDigits()})" else "ارسال دوباره کد")
            }
        }
    }
}

@Composable
private fun StepHeader(icon: ImageVector, title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(ProfileGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PrimaryButton(label: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RoundedCornerShape(27.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ProfileGreen, contentColor = Color.White)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
        else Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun resetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ProfileGreen,
    focusedLabelColor = ProfileGreen,
    focusedLeadingIconColor = ProfileGreen,
    cursorColor = ProfileGreen
)
