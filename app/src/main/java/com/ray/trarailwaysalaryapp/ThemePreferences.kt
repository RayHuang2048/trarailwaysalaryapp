package com.ray.trarailwaysalaryapp

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemePreferences {
    private const val PREFS_NAME = "theme_preferences"
    private const val KEY_THEME_MODE = "theme_mode"
    
    // Theme mode constants
    const val MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    const val MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO
    const val MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveThemeMode(context: Context, mode: Int) {
        getPreferences(context).edit().putInt(KEY_THEME_MODE, mode).apply()
    }
    
    fun getThemeMode(context: Context): Int {
        return getPreferences(context).getInt(KEY_THEME_MODE, MODE_SYSTEM)
    }
    
    fun applyTheme(context: Context) {
        val mode = getThemeMode(context)
        AppCompatDelegate.setDefaultNightMode(mode)
    }
    
    fun isDarkMode(context: Context): Boolean {
        val mode = getThemeMode(context)
        return when (mode) {
            MODE_DARK -> true
            MODE_LIGHT -> false
            else -> {
                // Check system setting
                val nightMode = context.resources.configuration.uiMode and 
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
}
