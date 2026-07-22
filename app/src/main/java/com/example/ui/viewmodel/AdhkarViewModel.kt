package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AdhkarDatabase
import com.example.data.local.DhikrProgressEntity
import com.example.data.local.TasbihSessionEntity
import com.example.data.model.AdhkarData
import com.example.data.model.AyahOfTheDay
import com.example.data.model.DhikrItem
import com.example.data.repository.AdhkarRepository
import com.example.data.repository.PreferenceRepository
import com.example.notifications.AdhkarNotificationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class AdhkarViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AdhkarDatabase.getDatabase(application)
    private val repository = AdhkarRepository(database.dhikrProgressDao(), database.tasbihSessionDao())
    private val prefs = PreferenceRepository(application)
    private val notificationManager = AdhkarNotificationManager(application)

    // Navigation and Search State
    private val _currentTab = MutableStateFlow("home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Preferences State
    private val _fontScale = MutableStateFlow(prefs.getFontScale())
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(prefs.isVibrationEnabled())
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.isSoundEnabled())
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.isNotificationsEnabled())
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _morningTime = MutableStateFlow(prefs.getMorningNotificationTime())
    val morningTime: StateFlow<String> = _morningTime.asStateFlow()

    private val _eveningTime = MutableStateFlow(prefs.getEveningNotificationTime())
    val eveningTime: StateFlow<String> = _eveningTime.asStateFlow()

    // Dynamic Ayah of the Day
    val ayahOfTheDay: AyahOfTheDay = getRotatedAyah()

    // Active Category Adhkar Flow
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentCategoryAdhkar: StateFlow<List<DhikrItem>> = _selectedCategoryId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getAdhkarByCategory(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProgress: StateFlow<List<DhikrProgressEntity>> = repository.getAllProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search Results for global search
    val searchResults: Flow<List<Pair<String, DhikrItem>>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            flowOf(emptyList())
        } else {
            // Flatten all categories with their titles to find matching dhikrs
            val flows = AdhkarData.adhkarList.map { (catId, items) ->
                repository.getAdhkarByCategory(catId).stateIn(viewModelScope, SharingStarted.Eagerly, items)
            }
            // Simple mapping since flows are local
            flowOf(
                AdhkarData.adhkarList.flatMap { (catId, items) ->
                    val catTitle = AdhkarData.categories.find { it.id == catId }?.title ?: ""
                    items.filter { 
                        it.arabicText.contains(query, ignoreCase = true) || 
                        it.persianTranslation.contains(query, ignoreCase = true) 
                    }.map { catTitle to it }
                }
            )
        }
    }

    // Virtual Tasbih State
    private val _tasbihCount = MutableStateFlow(0)
    val tasbihCount: StateFlow<Int> = _tasbihCount.asStateFlow()

    private val _selectedTasbihDhikr = MutableStateFlow("سبحان الله")
    val selectedTasbihDhikr: StateFlow<String> = _selectedTasbihDhikr.asStateFlow()

    val recentTasbihSessions: StateFlow<List<TasbihSessionEntity>> = repository.getRecentTasbihSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val toneGenerator = try {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
    } catch (e: Exception) {
        null
    }

    init {
        // Initial scheduling on app startup
        notificationManager.scheduleReminders()
    }

    // Navigation triggers
    fun selectTab(tab: String) {
        _currentTab.value = tab
        _selectedCategoryId.value = null // clear any active category stack
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Count operations
    fun incrementDhikr(categoryId: String, dhikrId: Int, targetCount: Int) {
        viewModelScope.launch {
            repository.incrementDhikrCount(categoryId, dhikrId, targetCount)
            playHapticAndAudio()
        }
    }

    fun resetCategoryProgress(categoryId: String) {
        viewModelScope.launch {
            repository.resetCategoryProgress(categoryId)
        }
    }

    fun resetSingleDhikr(categoryId: String, dhikrId: Int) {
        viewModelScope.launch {
            repository.resetSingleDhikr(categoryId, dhikrId)
        }
    }

    // Tasbih triggers
    fun incrementTasbih() {
        _tasbihCount.value += 1
        playHapticAndAudio()
    }

    fun resetTasbih() {
        _tasbihCount.value = 0
    }

    fun saveTasbihSession() {
        val count = _tasbihCount.value
        val name = _selectedTasbihDhikr.value
        if (count > 0) {
            viewModelScope.launch {
                repository.saveTasbihSession(name, count)
                _tasbihCount.value = 0
            }
        }
    }

    fun deleteTasbihSession(id: Int) {
        viewModelScope.launch {
            repository.deleteTasbihSession(id)
        }
    }

    fun selectTasbihDhikr(dhikr: String) {
        _selectedTasbihDhikr.value = dhikr
        resetTasbih()
    }

    // Preferences controllers
    fun updateFontScale(scale: Float) {
        prefs.setFontScale(scale)
        _fontScale.value = scale
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.setVibrationEnabled(enabled)
        _vibrationEnabled.value = enabled
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.setSoundEnabled(enabled)
        _soundEnabled.value = enabled
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.setNotificationsEnabled(enabled)
        _notificationsEnabled.value = enabled
        notificationManager.scheduleReminders()
    }

    fun updateMorningTime(time: String) {
        prefs.setMorningNotificationTime(time)
        _morningTime.value = time
        notificationManager.scheduleReminders()
    }

    fun updateEveningTime(time: String) {
        prefs.setEveningNotificationTime(time)
        _eveningTime.value = time
        notificationManager.scheduleReminders()
    }

    fun triggerTestNotification() {
        notificationManager.triggerTestNotification()
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            repository.resetAllProgress()
            repository.clearTasbihHistory()
            _tasbihCount.value = 0
        }
    }

    private fun getRotatedAyah(): AyahOfTheDay {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = dayOfYear % AdhkarData.ayatList.size
        return AdhkarData.ayatList[index]
    }

    private fun playHapticAndAudio() {
        // Haptic feedback
        if (_vibrationEnabled.value) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            } catch (e: Exception) {
                // ignore haptic exception to prevent any potential crash
            }
        }

        // Sound feedback
        if (_soundEnabled.value) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
