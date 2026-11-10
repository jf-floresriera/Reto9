package com.example.reto9.utils

import androidx.appcompat.app.AppCompatDelegate

/**
 * Aplica el modo de tema (claro / oscuro / sistema) a toda la app usando
 * AppCompatDelegate, que recrea las actividades con el tema correcto.
 */
object ThemeManager {

    fun apply(themeMode: Int) {
        val mode = when (themeMode) {
            PreferencesManager.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            PreferencesManager.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
