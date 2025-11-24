package com.example.ecolapor.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object ThemeManager {
    private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")

    val isDarkMode = mutableStateOf(false)

    fun getDarkModeFlow(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[IS_DARK_MODE] ?: false
        }
    }

    suspend fun toggleDarkMode(context: Context) {
        context.dataStore.edit { preferences ->
            val current = preferences[IS_DARK_MODE] ?: false
            preferences[IS_DARK_MODE] = !current
            isDarkMode.value = !current
        }
    }
}