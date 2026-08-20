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
import com.example.data.model.EmotionalAyah
import com.example.data.model.UserFeeling
import com.example.data.repository.AdhkarRepository
import com.example.data.repository.PreferenceRepository
import com.example.notifications.AdhkarNotificationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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

    private val _darkModeEnabled = MutableStateFlow(prefs.isDarkModeEnabled())
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled.asStateFlow()

    private val _customDhikr = MutableStateFlow(prefs.getCustomDhikr())
    val customDhikr: StateFlow<List<String>> = _customDhikr.asStateFlow()

    private val _favoriteDhikrKeys = MutableStateFlow(prefs.getFavoriteDhikrKeys())
    val favoriteDhikrKeys: StateFlow<Set<String>> = _favoriteDhikrKeys.asStateFlow()

    private val _activityDayKeys = MutableStateFlow(prefs.getActivityDayKeys())
    val activityDayKeys: StateFlow<Set<Long>> = _activityDayKeys.asStateFlow()

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

    private val _dailyChecklistCompletedIds = MutableStateFlow(prefs.getDailyChecklistCompletedIds())
    val dailyChecklistCompletedIds: StateFlow<Set<String>> = _dailyChecklistCompletedIds.asStateFlow()

    private val _checklistCompletionCounts = MutableStateFlow(prefs.getChecklistCompletionCounts(30))
    val checklistCompletionCounts: StateFlow<Map<Long, Int>> = _checklistCompletionCounts.asStateFlow()

    // Dynamic Ayah of the Day
    val ayahOfTheDay: AyahOfTheDay = getRotatedAyah()

    private val _selectedFeeling = MutableStateFlow(UserFeeling.fromStorageKey(prefs.getSelectedFeeling()))
    val selectedFeeling: StateFlow<UserFeeling?> = _selectedFeeling.asStateFlow()
    private val _emotionalAyah = MutableStateFlow(getEmotionalAyah(_selectedFeeling.value))
    val emotionalAyah: StateFlow<EmotionalAyah?> = _emotionalAyah.asStateFlow()

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
        viewModelScope.launch {
            while (true) {
                val now = Calendar.getInstance()
                val nextDay = (now.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                delay((nextDay.timeInMillis - now.timeInMillis).coerceAtLeast(1_000L))
                prefs.clearSelectedFeeling()
                _selectedFeeling.value = null
                _emotionalAyah.value = null
                _dailyChecklistCompletedIds.value = prefs.getDailyChecklistCompletedIds()
                _checklistCompletionCounts.value = prefs.getChecklistCompletionCounts(30)
            }
        }
    }

    // Navigation triggers
    fun selectTab(tab: String) {
        _currentTab.value = tab
        _selectedCategoryId.value = null // clear any active category stack
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun leaveCategory(categoryId: String) {
        _selectedCategoryId.value = null
        viewModelScope.launch {
            repository.resetCompletedProgress(categoryId)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Count operations
    fun incrementDhikr(categoryId: String, dhikrId: Int, targetCount: Int) {
        viewModelScope.launch {
            _activityDayKeys.value = prefs.markActivityToday()
            val categoryCompleted = repository.incrementDhikrCount(categoryId, dhikrId, targetCount)
            if (categoryCompleted) {
                prefs.markAdhkarCompletedToday(categoryId)
            }
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

    fun decrementDhikr(categoryId: String, dhikrId: Int) {
        viewModelScope.launch {
            repository.decrementDhikrCount(categoryId, dhikrId)
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
                _activityDayKeys.value = prefs.markActivityToday()
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

    fun addCustomDhikr(text: String) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return
        _customDhikr.value = prefs.addCustomDhikr(normalized)
        selectTasbihDhikr(normalized)
    }

    fun removeCustomDhikr(text: String, fallbackDhikr: String) {
        _customDhikr.value = prefs.removeCustomDhikr(text)
        if (_selectedTasbihDhikr.value == text) {
            selectTasbihDhikr(fallbackDhikr)
        }
    }

    fun toggleFavoriteDhikr(categoryId: String, dhikrId: Int) {
        _favoriteDhikrKeys.value = prefs.toggleFavoriteDhikr("$categoryId:$dhikrId")
    }

    // Preferences controllers
    fun updateFontScale(scale: Float) {
        prefs.setFontScale(scale)
        _fontScale.value = scale
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.setDarkModeEnabled(enabled)
        _darkModeEnabled.value = enabled
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

    fun setDailyChecklistItemCompleted(itemId: String, completed: Boolean) {
        _dailyChecklistCompletedIds.value =
            prefs.setDailyChecklistItemCompleted(
                dayKey = currentChecklistDayKey(),
                itemId = itemId,
                completed = completed
            )
        _checklistCompletionCounts.value = prefs.getChecklistCompletionCounts(30)
        if (completed) {
            _activityDayKeys.value = prefs.markActivityToday()
        }
    }

    private fun currentChecklistDayKey(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun selectFeeling(feeling: UserFeeling) {
        prefs.setSelectedFeeling(feeling.storageKey)
        _selectedFeeling.value = feeling
        _emotionalAyah.value = getEmotionalAyah(feeling)
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

    private fun getEmotionalAyah(feeling: UserFeeling?): EmotionalAyah? {
        if (feeling == null) return null
        val choices = AdhkarData.emotionalAyat.filter { it.feeling == feeling }
        if (choices.isEmpty()) return null
        val calendar = Calendar.getInstance()
        val dayKey = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
        return choices[Math.floorMod(dayKey + feeling.ordinal * 31, choices.size)]
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
