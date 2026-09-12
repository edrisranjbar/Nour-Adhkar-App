package com.example.ui.screens
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text

import android.content.Context
import android.hardware.*
import android.location.Location
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.prayer.qiblaBearing
import com.example.prayer.qiblaTurn
import com.example.ui.viewmodel.AdhkarViewModel
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.util.Locale

private fun degrees(value: Number) = value.toDouble().roundToInt().toString()
    .map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("") + "°"

@Composable
fun QiblaScreen(viewModel: AdhkarViewModel, innerPadding: PaddingValues) {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val saved by viewModel.prayerSettings.collectAsState()
    var fix by remember { mutableStateOf<Location?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val location = fix ?: remember(saved) {
        if (saved.isValid()) Location("saved").apply {
            latitude = saved.latitude
            longitude = saved.longitude
        } else null
    }
    var city by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(fix, language) {
        city = null
        val current = fix ?: return@LaunchedEffect
        city = withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) return@withContext null
                @Suppress("DEPRECATION")
                val address = Geocoder(context, Locale.forLanguageTag(language.code))
                    .getFromLocation(current.latitude, current.longitude, 1)?.firstOrNull()
                address?.locality?.takeIf { it.isNotBlank() }
                    ?: address?.subAdminArea?.takeIf { it.isNotBlank() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) { null }
        }
    }
    var compassRetry by remember { mutableIntStateOf(0) }
    val compass = rememberCompass(compassRetry)
    val bearing = location?.let { qiblaBearing(it.latitude, it.longitude) }
    val declination = remember(location) {
        location?.let { GeomagneticField(it.latitude.toFloat(), it.longitude.toFloat(),
            it.altitude.toFloat(), System.currentTimeMillis()).declination } ?: 0f
    }
    val turn = if (bearing != null && compass.heading != null)
        qiblaTurn(bearing, compass.heading.toDouble() + declination) else null
    val ready = turn != null && !compass.unreliable && compass.flat
    val aligned = ready && abs(turn ?: 360f) <= 5f
    val status = when {
        location == null -> "موقعیت را مشخص کنید"
        !compass.available -> "حسگر قطب‌نما در دسترس نیست"
        compass.connectionFailed -> "اتصال به قطب‌نما برقرار نشد؛ دوباره تلاش کنید"
        compass.heading == null -> "در حال اتصال به قطب‌نما…"
        !compass.flat -> "گوشی را صاف نگه دارید"
        compass.unreliable -> "قطب‌نما را کالیبره کنید"
        abs(turn!!) <= 5f -> "بالای گوشی رو به قبله است"
        turn > 0 -> "گوشی را ${degrees(turn)} به راست بچرخانید"
        else -> "گوشی را ${degrees(abs(turn))} به چپ بچرخانید"
    }
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically) {
            val cityLabel = city ?: if (fix != null) "موقعیت فعلی" else saved.location.takeIf {
                fix == null && it.isNotBlank() && it != "موقعیت فعلی"
            }
            if (cityLabel != null) Text(cityLabel, color = muted,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                textAlign = TextAlign.Left)
            DetectPrayerLocationButton(
                label = "به‌روزرسانی موقعیت", iconOnly = true,
                onLocation = { fix = it; error = null }, onError = { error = it }
            )
        }
        Box(Modifier.widthIn(max = 340.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().semantics { contentDescription = language.text(status) }) {
                val radius = size.minDimension * 0.43f
                drawCircle(outline, radius, style = Stroke(2.dp.toPx()))
                repeat(36) { tick ->
                    rotate(tick * 10f) {
                        drawLine(outline, Offset(center.x, center.y - radius),
                            Offset(center.x, center.y - radius + if (tick % 9 == 0) 18.dp.toPx() else 7.dp.toPx()),
                            2.dp.toPx())
                    }
                }
                if (turn != null) rotate(turn) {
                    val arrow = Path().apply {
                        moveTo(center.x, center.y - radius * 0.72f)
                        lineTo(center.x + radius * 0.18f, center.y + radius * 0.25f)
                        lineTo(center.x, center.y + radius * 0.10f)
                        lineTo(center.x - radius * 0.18f, center.y + radius * 0.25f)
                        close()
                    }
                    if (aligned) drawCircle(primary.copy(alpha = 0.14f), radius * 0.24f,
                        Offset(center.x, center.y - radius * 0.22f))
                    drawPath(arrow, if (aligned) primary else muted.copy(alpha = 0.35f))
                }
                drawCircle(primary, 5.dp.toPx(), Offset(center.x, center.y - radius - 10.dp.toPx()))
            }
            if (turn == null) Text("قبله", color = muted, style = MaterialTheme.typography.headlineMedium)
        }
        Text(status, color = if (aligned) primary else muted, style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center)
        if (compass.connectionFailed) TextButton(onClick = { compassRetry++ }) {
            Text("تلاش دوباره")
        }
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    }
}

private data class CompassReading(val available: Boolean, val heading: Float? = null,
    val unreliable: Boolean = true, val flat: Boolean = true,
    val connectionFailed: Boolean = false)

@Suppress("DEPRECATION")
@Composable
private fun rememberCompass(retry: Int): CompassReading {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    // Try every north-referenced provider, including non-default hardware variants.
    // Game rotation vectors deliberately do not qualify: they have no magnetic north.
    val candidates = remember(manager) {
        val vectors = (manager.getSensorList(Sensor.TYPE_ROTATION_VECTOR) +
            manager.getSensorList(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)).map { listOf(it) }
        val gravity = manager.getSensorList(Sensor.TYPE_ACCELEROMETER) +
            manager.getSensorList(Sensor.TYPE_GRAVITY)
        val magnetic = manager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD)
        vectors + gravity.flatMap { a -> magnetic.map { m -> listOf(a, m) } }
    }
    val available = candidates.isNotEmpty()
    var reading by remember { mutableStateOf(CompassReading(available)) }
    DisposableEffect(manager, lifecycle, retry) {
        val matrix = FloatArray(9)
        val remapped = FloatArray(9)
        val angles = FloatArray(3)
        var acceleration: FloatArray? = null
        var field: FloatArray? = null
        var filtered: Float? = null
        val handler = Handler(Looper.getMainLooper())
        var activeSensors = emptyList<Sensor>()
        var candidateIndex = 0
        var timeout: Runnable? = null
        val windows = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor in activeSensors && sensor.type != Sensor.TYPE_ACCELEROMETER &&
                    sensor.type != Sensor.TYPE_GRAVITY)
                    reading = reading.copy(unreliable = accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)
            }
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor !in activeSensors) return
                if (activeSensors.size == 1) SensorManager.getRotationMatrixFromVector(matrix, event.values)
                else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER ||
                        event.sensor.type == Sensor.TYPE_GRAVITY) acceleration = event.values.copyOf()
                    if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) field = event.values.copyOf()
                    val a = acceleration ?: return
                    val m = field ?: return
                    if (!SensorManager.getRotationMatrix(matrix, null, a, m)) return
                }
                val axes = when (windows.defaultDisplay.rotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(matrix, axes.first, axes.second, remapped)
                SensorManager.getOrientation(remapped, angles)
                timeout?.let { handler.removeCallbacks(it) }
                timeout = null
                val heading = Math.toDegrees(angles[0].toDouble()).toFloat()
                filtered = filtered?.let { it + 0.2f * qiblaTurn(heading.toDouble(), it.toDouble()) } ?: heading
                val unreliable = if (event.sensor.type != Sensor.TYPE_ACCELEROMETER &&
                    event.sensor.type != Sensor.TYPE_GRAVITY)
                    event.accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM else reading.unreliable
                reading = CompassReading(true, filtered, unreliable,
                    abs(angles[1]) < 0.6f && abs(angles[2]) < 0.6f)
            }
        }
        fun stop() {
            timeout?.let { handler.removeCallbacks(it) }
            timeout = null
            activeSensors = emptyList()
            manager.unregisterListener(listener)
            filtered = null; acceleration = null; field = null
            reading = CompassReading(available)
        }
        fun tryNext() {
            manager.unregisterListener(listener)
            activeSensors = emptyList()
            filtered = null; acceleration = null; field = null
            while (candidateIndex < candidates.size) {
                val sensors = candidates[candidateIndex++]
                val registered = sensors.all {
                    manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
                }
                if (registered) {
                    activeSensors = sensors
                    reading = CompassReading(available)
                    // Some virtual providers register successfully but never deliver a matrix.
                    timeout = Runnable { tryNext() }.also { handler.postDelayed(it, 3000L) }
                    return
                }
                manager.unregisterListener(listener)
            }
            reading = CompassReading(available, connectionFailed = available)
        }
        fun start() {
            stop()
            candidateIndex = 0
            if (available) tryNext()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE -> stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
        onDispose { lifecycle.removeObserver(observer); stop() }
    }
    return reading
}
