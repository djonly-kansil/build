package com.taloarane.appcontroll.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("app_controll_prefs")

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class Settings(
    val language: String = "id",
    val theme: ThemeMode = ThemeMode.DARK,
    val keepScreenOn: Boolean = false,
    val whitelist: Set<String> = emptySet(),
    val onboarded: Boolean = false,
)

class Prefs(private val context: Context) {

    private val kLang = stringPreferencesKey("language")
    private val kTheme = stringPreferencesKey("theme")
    private val kKeepOn = booleanPreferencesKey("keep_screen_on")
    private val kWhitelist = stringSetPreferencesKey("whitelist")
    private val kOnboarded = booleanPreferencesKey("onboarded")

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            language = p[kLang] ?: "id",
            theme = runCatching { ThemeMode.valueOf(p[kTheme] ?: "DARK") }.getOrDefault(ThemeMode.DARK),
            keepScreenOn = p[kKeepOn] ?: false,
            whitelist = p[kWhitelist] ?: emptySet(),
            onboarded = p[kOnboarded] ?: false,
        )
    }

    suspend fun setLanguage(value: String) = context.dataStore.edit { it[kLang] = value }
    suspend fun setTheme(value: ThemeMode) = context.dataStore.edit { it[kTheme] = value.name }
    suspend fun setKeepScreenOn(value: Boolean) = context.dataStore.edit { it[kKeepOn] = value }
    suspend fun setOnboarded(value: Boolean) = context.dataStore.edit { it[kOnboarded] = value }

    suspend fun toggleWhitelist(pkg: String) = context.dataStore.edit { p ->
        val current = p[kWhitelist] ?: emptySet()
        p[kWhitelist] = if (pkg in current) current - pkg else current + pkg
    }
}
