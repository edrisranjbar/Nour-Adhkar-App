package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.example.R
import com.example.data.repository.AuthException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Thrown when the user closes Google's account chooser; callers show nothing. */
class GoogleSignInCancelled : Exception()

val isGoogleSignInConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

/**
 * Standard "Sign in with Google" button (Google branding: unmodified four-colour G, neutral
 * light/dark surface, 1dp outline, pill shape, medium-weight label).
 */
@Composable
fun GoogleSignInButton(onClick: () -> Unit, loading: Boolean, enabled: Boolean, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, if (dark) Color(0xFF8E918F) else Color(0xFF747775)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (dark) Color(0xFF131314) else Color.White,
            contentColor = if (dark) Color(0xFFE3E3E3) else Color(0xFF1F1F1F)
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Image(painterResource(R.drawable.ic_google_logo), contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(12.dp))
            Text("ورود با گوگل", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Opens Google's account chooser and returns an ID token for the API to verify. */
suspend fun requestGoogleIdToken(context: Context): String {
    // LanguageProvider wraps the context; Credential Manager needs the hosting Activity.
    var activity: Context = context
    while (activity !is Activity && activity is ContextWrapper) activity = activity.baseContext

    val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val credential = try {
        CredentialManager.create(activity).getCredential(activity, request).credential
    } catch (e: GetCredentialCancellationException) {
        throw GoogleSignInCancelled()
    } catch (e: NoCredentialException) {
        throw AuthException("هیچ حساب گوگلی روی این گوشی پیدا نشد. ابتدا از تنظیمات گوشی یک حساب گوگل اضافه کنید.")
    } catch (e: GetCredentialException) {
        throw AuthException("ورود با گوگل انجام نشد. اتصال اینترنت و سرویس‌های گوگل گوشی را بررسی کنید.")
    }
    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
    throw AuthException("ورود با گوگل انجام نشد. لطفاً دوباره تلاش کنید.")
}
