package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

@Composable
fun DetectPrayerLocationButton(
    label: String = "دریافت موقعیت فعلی با GPS",
    iconOnly: Boolean = false,
    onLocation: (Location) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locating by remember { mutableStateOf(false) }
    val latestLocation by rememberUpdatedState(onLocation)
    val latestError by rememberUpdatedState(onError)
    fun detect() {
        if (locating) return
        scope.launch {
            locating = true
            try {
                val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (!LocationManagerCompat.isLocationEnabled(manager)) {
                    latestError("مکان‌یابی گوشی هنوز خاموش است.")
                    return@launch
                }
                val precise = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val providers = listOfNotNull(
                    LocationManager.GPS_PROVIDER.takeIf { precise && manager.isProviderEnabled(it) },
                    LocationManager.NETWORK_PROVIDER.takeIf { manager.isProviderEnabled(it) }
                )
                if (providers.isEmpty()) {
                    latestError("GPS یا مکان‌یابی شبکه در دسترس نیست؛ تنظیمات مکان گوشی را بررسی کنید.")
                } else {
                    val result = withTimeoutOrNull(25_000) { currentLocation(context, manager, providers) }
                    if (result != null) latestLocation(result)
                    else latestError("موقعیت دریافت نشد. در فضای باز دوباره تلاش کنید یا نام شهر را دستی وارد کنید.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                latestError("دسترسی به موقعیت ممکن نیست؛ مجوز مکان را بررسی کنید یا نام شهر را دستی وارد کنید.")
            } finally {
                locating = false
            }
        }
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) detect()
        else latestError("مجوز مکان داده نشد. آن را در تنظیمات گوشی فعال کنید یا نام شهر را دستی وارد کنید.")
    }
    val locationSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (LocationManagerCompat.isLocationEnabled(manager)) detect()
        else latestError("برای دریافت موقعیت، GPS یا مکان‌یابی گوشی را روشن کنید.")
    }
    val requestLocation: () -> Unit = {
        val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(manager)) {
            locationSettings.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else if (granted) detect()
        else permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    if (iconOnly) {
        IconButton(onClick = requestLocation, enabled = !locating) {
            if (locating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.MyLocation, contentDescription = label)
        }
    } else OutlinedButton(onClick = requestLocation, enabled = !locating, modifier = Modifier.fillMaxWidth()) {
        if (locating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else Icon(Icons.Default.MyLocation, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (locating) "در حال دریافت موقعیت…" else label)
    }
}

// Only active while the user requests a fix; all providers stop on success,
// timeout, or leaving the screen. No background location permission is needed.
private suspend fun currentLocation(context: Context, manager: LocationManager, providers: List<String>): Location? {
    val signals = providers.map { CancellationSignal() }
    try {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { signals.forEach { it.cancel() } }
            var remaining = providers.size
            providers.forEachIndexed { index, provider ->
                LocationManagerCompat.getCurrentLocation(manager, provider, signals[index], ContextCompat.getMainExecutor(context)) { location ->
                    remaining--
                    if (continuation.isActive && (location != null || remaining == 0)) continuation.resume(location)
                }
            }
        }
    } finally {
        signals.forEach { it.cancel() }
    }
}
