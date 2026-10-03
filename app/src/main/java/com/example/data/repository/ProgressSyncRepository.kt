package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import org.json.JSONObject
import java.util.UUID

enum class ProgressSyncError { CONNECTION, SIGN_IN_REQUIRED, SERVER, RATE_LIMITED, INVALID_DATA, TOO_LARGE, UNKNOWN }

data class ProgressSyncState(val busy: Boolean = false, val lastSynced: Long = 0, val error: Boolean = false, val errorReason: ProgressSyncError? = null)

/** Automatic signed-in account backup. Offline edits are journalled and retried on next use. */
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
                    ProgressSyncState(lastSynced = it.getLong("last_synced", 0))
                }
                if (user != null) sync(app)
            }
        }
        scope.launch { while (isActive) { delay(30_000); if (foreground && AccountRepository.user.value != null) sync(app) } }
    }

    fun onForeground(context: Context, active: Boolean) {
        foreground = active
        if (AccountRepository.user.value != null) sync(context)
    }

    fun sync(context: Context) {
        val app = context.applicationContext
        scope.launch {
            if (!mutex.tryLock()) return@launch
            var syncingEmail: String? = null
            try {
                val email = AccountRepository.user.value?.email ?: return@launch
                syncingEmail = email
                val token = AccountRepository.token(app) ?: return@launch
                val prefs = journal(app, email)
                _state.value = _state.value.copy(busy = true, error = false, errorReason = null)
                val snapshot = ProgressSnapshot(app)
                var baseline = JSONObject(prefs.getString("baseline", "{}").orEmpty())
                var records = JSONObject(prefs.getString("records", "{}").orEmpty())
                val device = prefs.getString("device", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("device", it).commit() }
                val local = snapshot.read()
                records = ProgressRecords.capture(baseline, records, local, device, System.currentTimeMillis())
                baseline = local
                // Persist pending changes even if the phone goes offline or the process is killed.
                prefs.edit().putString("records", records.toString()).putString("baseline", baseline.toString()).commit()
                val response = withContext(Dispatchers.IO) {
                    ProgressSyncApi().sync(token, ProgressRecords.array(records)) { AccountRepository.refreshToken(app, it) }
                }
                if (AccountRepository.token(app) != response.token || AccountRepository.user.value?.email != email) return@launch
                val latest = snapshot.read()
                records = ProgressRecords.capture(baseline, records, latest, device, System.currentTimeMillis())
                records = ProgressRecords.merge(records, response.records)
                val merged = ProgressRecords.snapshot(records)
                snapshot.apply(latest, merged)
                val actual = snapshot.read()
                records = ProgressRecords.capture(merged, records, actual, device, System.currentTimeMillis())
                val now = System.currentTimeMillis()
                prefs.edit().putString("records", records.toString()).putString("baseline", actual.toString()).putLong("last_synced", now).commit()
                _state.value = ProgressSyncState(lastSynced = now)
                if (latest.toString() != actual.toString()) {
                    _restored.value++
                    com.example.widget.ChecklistWidgetProvider.updateAll(app)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (syncingEmail != null && AccountRepository.user.value?.email == syncingEmail)
                    _state.value = _state.value.copy(busy = false, error = true, errorReason = syncError(failure))
            }
            finally { _state.value = _state.value.copy(busy = false); mutex.unlock() }
        }
    }
}

internal fun syncError(failure: Exception): ProgressSyncError {
    val status = when (failure) {
        is ProgressSyncHttpException -> failure.status
        is AuthException -> failure.status
        else -> null
    }
    return when (status) {
        401, 403 -> ProgressSyncError.SIGN_IN_REQUIRED
        413 -> ProgressSyncError.TOO_LARGE
        422 -> ProgressSyncError.INVALID_DATA
        429 -> ProgressSyncError.RATE_LIMITED
        null -> if (failure is java.io.IOException || failure is AuthException) ProgressSyncError.CONNECTION else ProgressSyncError.UNKNOWN
        else -> if (status in 500..599) ProgressSyncError.SERVER else ProgressSyncError.UNKNOWN
    }
}

/** One backup-excluded file; account-scoped keys avoid mixing journals on a shared device. */
private class ProgressJournal(context: Context, email: String) {
    private val prefs = context.getSharedPreferences("progress_sync", Context.MODE_PRIVATE)
    private val prefix = java.security.MessageDigest.getInstance("SHA-256").digest(email.toByteArray())
        .joinToString("") { "%02x".format(it) } + ":"
    fun getString(key: String, default: String?) = prefs.getString(prefix + key, default)
    fun getLong(key: String, default: Long) = prefs.getLong(prefix + key, default)
    fun edit() = Editor(prefs.edit())
    inner class Editor(private val editor: android.content.SharedPreferences.Editor) {
        fun putString(key: String, value: String) = apply { editor.putString(prefix + key, value) }
        fun putLong(key: String, value: Long) = apply { editor.putLong(prefix + key, value) }
        fun commit() = editor.commit()
    }
}
