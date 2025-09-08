package com.example.e_zuka.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.e_zuka.data.model.AppThemeConfig
import com.example.e_zuka.data.model.ThemeMode
import com.example.e_zuka.data.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ThemeSettingsViewModel(context: Context) : ViewModel() {
    private val appSettings = AppSettings(context)

    private val _themeConfig = MutableStateFlow(AppThemeConfig())
    val themeConfig: StateFlow<AppThemeConfig> = _themeConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // デフォルト値の定数
    companion object {
        const val DEFAULT_FONT_SCALE = 1.0f
        const val DEFAULT_HIGH_CONTRAST = false
        const val DEFAULT_SENIOR_MODE = false
        val DEFAULT_THEME_MODE = ThemeMode.SYSTEM
    }

    init {
        viewModelScope.launch {
            combine(
                appSettings.themeMode,
                appSettings.fontScale,
                appSettings.highContrast,
                appSettings.seniorMode
            ) { themeMode, fontScale, highContrast, seniorMode ->
                AppThemeConfig(
                    themeMode = ThemeMode.fromString(themeMode),
                    fontScale = fontScale,
                    isHighContrast = highContrast,
                    isSeniorMode = seniorMode
                )
            }.collect { config ->
                _themeConfig.value = config
            }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                appSettings.setThemeMode(mode.name.lowercase())
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateFontScale(scale: Float) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                appSettings.setFontScale(scale)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                appSettings.setHighContrast(enabled)
                if (!enabled && _themeConfig.value.isSeniorMode) {
                    // 高コントラストモードをオフにした時に高齢者モードがオンの場合は、高齢者モードもオフにする
                    updateSeniorMode(false)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSeniorMode(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (enabled) {
                    // 高齢者モードがオンの場合、自動的に以下の設定を適用
                    appSettings.setFontScale(1.2f) // 文字サイズを大きめに
                    appSettings.setHighContrast(true) // コントラストを高めに
                    appSettings.setSeniorMode(true)
                } else {
                    // 高齢者モードがオフの場合、全ての設定をデフォルト値に戻す
                    resetToDefaults()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 全ての設定をデフォルト値に戻す
    private suspend fun resetToDefaults() {
        appSettings.setFontScale(DEFAULT_FONT_SCALE)
        appSettings.setHighContrast(DEFAULT_HIGH_CONTRAST)
        appSettings.setSeniorMode(DEFAULT_SENIOR_MODE)
        appSettings.setThemeMode(DEFAULT_THEME_MODE.name.lowercase())
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ThemeSettingsViewModel::class.java)) {
                return ThemeSettingsViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
