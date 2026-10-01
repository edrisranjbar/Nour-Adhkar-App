package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class ProgressSyncState(val enabled: Boolean = false, val busy: Boolean = false, val lastSynced: Long = 0, val error: Boolean = false)

/** Optional account backup. Offline edits are journalled before requests and retried on next use. */
object ProgressSyncRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private var initialized = false
    private var foreground = true
    private val _state = MutableStateFlow(ProgressSyncState())
    val state = _state.asStateFlow()
    private val _restored = MutableStateFlow(0L)
    val restored = _restored.asStateFlow()

    private fun journal(context: Context, email: String) = ProgressJournal(context, email)

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val app = context.applicationContext
        scope.launch {
            AccountRepository.user.collect { user ->
                _state.value = if (user == null) ProgressSyncState() else journal(app, user.email).let {
                    ProgressSyncState(it.getBoolean("enabled", false), lastSynced = it.getLong("last_synced", 0))
                }
                if (_state.value.enabled) sync(app)
            }
        }
        scope.launch { while (isActive) { delay(30_000); if (foreground && _state.value.enabled) sync(app) } }
    }

    fun onForeground(context: Context, active: Boolean) {
        foreground = active
        if (_state.value.enabled) sync(context)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val email = AccountRepository.user.value?.email ?: return
        journal(context, email).edit().putBoolean("enabled", enabled).commit()
        _state.value = _state.value.copy(enabled = enabled)
        if (enabled) sync(context)
    }

    fun sync(context: Context) {
        val app = context.applicationContext
        scope.launch {
            if (!mutex.tryLock()) return@launch
            try {
                val email = AccountRepository.user.value?.email ?: return@launch
                val token = AccountRepository.token(app) ?: return@launch
                val prefs = journal(app, email)
                if (!prefs.getBoolean("enabled", false)) return@launch
                _state.value = _state.value.copy(busy = true, error = false)
                val snapshot = ProgressSnapshot(app)
                var baseline = JSONObject(prefs.getString("baseline", "{}").orEmpty())
                var records = JSONObject(prefs.getString("records", "{}").orEmpty())
                val device = prefs.getString("device", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("device", it).commit() }
                val local = snapshot.read()
                records = ProgressRecords.capture(baseline, records, local, device, System.currentTimeMillis())
                baseline = local
                // Persist pending changes even if the phone goes offline or the process is killed.
                prefs.edit().putString("records", records.toString()).putString("baseline", baseline.toString()).commit()
                val response = withContext(Dispatchers.IO) { request(token, ProgressRecords.array(records)) }
                if (AccountRepository.token(app) != token || !prefs.getBoolean("enabled", false)) return@launch
                val latest = snapshot.read()
                records = ProgressRecords.capture(baseline, records, latest, device, System.currentTimeMillis())
                records = ProgressRecords.merge(records, response)
                val merged = ProgressRecords.snapshot(records)
                snapshot.apply(latest, merged)
                val actual = snapshot.read()
                records = ProgressRecords.capture(merged, records, actual, device, System.currentTimeMillis())
                val now = System.currentTimeMillis()
                prefs.edit().putString("records", records.toString()).putString("baseline", actual.toString()).putLong("last_synced", now).commit()
                _state.value = ProgressSyncState(enabled = true, lastSynced = now)
                if (latest.toString() != actual.toString()) {
                    _restored.value++
                    com.example.widget.ChecklistWidgetProvider.updateAll(app)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.value = _state.value.copy(busy = false, error = true) }
            finally { _state.value = _state.value.copy(busy = false); mutex.unlock() }
        }
    }

    private fun request(token: String, records: JSONArray): JSONArray {
        val connection = URL("https://api.adhkar.ir/api/progress/sync").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            val payload = JSONObject().put("version", 1).put("records", records).toString().toByteArray()
            require(payload.size <= 4_000_000)
            connection.outputStream.use { it.write(payload) }
            check(connection.responseCode in 200..299)
            val body = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() <= 4_000_000) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                val bytes = output.toByteArray()
                require(bytes.size <= 4_000_000)
                String(bytes, Charsets.UTF_8)
            }
            return JSONObject(body).getJSONArray("records")
        } finally { connection.disconnect() }
    }
}

/** One backup-excluded file; account-scoped keys avoid mixing journals on a shared device. */
private class ProgressJournal(context: Context, email: String) {
    private val prefs = context.getSharedPreferences("progress_sync", Context.MODE_PRIVATE)
    private val prefix = java.security.MessageDigest.getInstance("SHA-256").digest(email.toByteArray())
        .joinToString("") { "%02x".format(it) } + ":"
    fun getString(key: String, default: String?) = prefs.getString(prefix + key, default)
    fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(prefix + key, default)
    fun getLong(key: String, default: Long) = prefs.getLong(prefix + key, default)
    fun edit() = Editor(prefs.edit())
    inner class Editor(private val editor: android.content.SharedPreferences.Editor) {
        fun putString(key: String, value: String) = apply { editor.putString(prefix + key, value) }
        fun putBoolean(key: String, value: Boolean) = apply { editor.putBoolean(prefix + key, value) }
        fun putLong(key: String, value: Long) = apply { editor.putLong(prefix + key, value) }
        fun commit() = editor.commit()
    }
}
