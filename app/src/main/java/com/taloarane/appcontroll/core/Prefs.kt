package com.taloarane.appcontroll.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { DARK, LIGHT, SYSTEM }

class Prefs private constructor(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("appcontroll", Context.MODE_PRIVATE)

    var language by mutableStateOf(sp.getString("lang", "id") ?: "id")
    var themeMode by mutableStateOf(
        runCatching { ThemeMode.valueOf(sp.getString("theme", "DARK")!!) }.getOrDefault(ThemeMode.DARK)
    )
    var keepScreenOn by mutableStateOf(sp.getBoolean("keep_screen_on", false))
    var whitelist by mutableStateOf(sp.getStringSet("whitelist", emptySet())!!.toSet())

    fun setLanguage(v: String) {
        language = v; sp.edit().putString("lang", v).apply()
    }

    fun setTheme(v: ThemeMode) {
        themeMode = v; sp.edit().putString("theme", v.name).apply()
    }

    fun setKeepScreenOn(v: Boolean) {
        keepScreenOn = v; sp.edit().putBoolean("keep_screen_on", v).apply()
    }

    fun toggleWhitelist(pkg: String) {
        whitelist = if (whitelist.contains(pkg)) whitelist - pkg else whitelist + pkg
        sp.edit().putStringSet("whitelist", whitelist).apply()
    }

    val strings: L get() = if (language == "en") EN else ID

    companion object {
        @Volatile
        private var inst: Prefs? = null
        fun get(context: Context): Prefs = inst ?: synchronized(this) {
            inst ?: Prefs(context).also { inst = it }
        }
    }
}
