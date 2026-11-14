package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.AppLanguage
import com.example.data.fs.StorageStats
import com.example.data.prefs.AppSettings
import com.example.data.prefs.EditorFontFamily
import com.example.data.prefs.PreviewBehavior
import com.example.data.prefs.SettingsRepository
import com.example.data.prefs.ThemeMode
import com.example.data.repository.IdeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val ideRepository: IdeRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    private val _storageStats = MutableStateFlow(StorageStats(0, 0, 0L))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    fun refreshStorageStats() {
        viewModelScope.launch {
            val projects = ideRepository.projects.first()
            _storageStats.value = ideRepository.getStorageStats(projects)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setFontSize(sizeSp: Float) {
        viewModelScope.launch { settingsRepository.setFontSize(sizeSp) }
    }

    fun setFontFamily(family: EditorFontFamily) {
        viewModelScope.launch { settingsRepository.setFontFamily(family) }
    }

    fun setAutoSave(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoSave(enabled) }
    }

    fun setLineNumbers(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLineNumbers(enabled) }
    }

    fun setWordWrap(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWordWrap(enabled) }
    }

    fun setPreviewBehavior(behavior: PreviewBehavior) {
        viewModelScope.launch { settingsRepository.setPreviewBehavior(behavior) }
    }

    fun setServerPort(port: Int) {
        viewModelScope.launch { settingsRepository.setServerPort(port) }
    }

    fun setToolbarSymbols(symbols: String) {
        viewModelScope.launch { settingsRepository.setToolbarSymbols(symbols) }
    }

    suspend fun clearCache(): Boolean = ideRepository.clearCache()

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val ideRepository: IdeRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settingsRepository, ideRepository) as T
    }
}
