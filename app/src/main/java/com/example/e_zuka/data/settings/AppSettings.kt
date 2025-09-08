package com.example.e_zuka.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppSettings(private val context: Context) {
    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val SENIOR_MODE = booleanPreferencesKey("senior_mode")
    }

    // Theme mode settings
    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "system"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    // Font scale settings
    val fontScale: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[FONT_SCALE] ?: 1.0f
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SCALE] = scale
        }
    }

    // High contrast mode settings
    val highContrast: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HIGH_CONTRAST] ?: false
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIGH_CONTRAST] = enabled
        }
    }

    // Senior mode settings
    val seniorMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SENIOR_MODE] ?: false
    }

    suspend fun setSeniorMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SENIOR_MODE] = enabled
        }
    }
}
