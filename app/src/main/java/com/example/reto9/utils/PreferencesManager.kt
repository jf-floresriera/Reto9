package com.example.reto9.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Maneja las preferencias de la app: el radio de búsqueda en kilómetros
 * y el tipo de punto de interés seleccionado.
 */
class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("Reto9Prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_RADIUS_KM = "radius_km"
        private const val KEY_FILTER_TYPE = "filter_type"
    }

    // Radio de búsqueda en kilómetros (por defecto 2 km).
    var radiusKm: Int
        get() = prefs.getInt(KEY_RADIUS_KM, 2)
        set(value) = prefs.edit().putInt(KEY_RADIUS_KM, value).apply()

    // Filtro de categoría seleccionado.
    // 0=Todos, 1=Hospitales, 2=Turístico, 3=Restaurantes, 4=Farmacias, 5=Escuelas
    var filterType: Int
        get() = prefs.getInt(KEY_FILTER_TYPE, 0)
        set(value) = prefs.edit().putInt(KEY_FILTER_TYPE, value).apply()
}
