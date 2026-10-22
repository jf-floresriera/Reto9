package com.example.reto9.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("Reto9Prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_RADIUS_KM = "radius_km"
        private const val KEY_FILTER_TYPE = "filter_type"
    }

    // Guardar y obtener el radio en kilómetros (por defecto 5 km)
    var radiusKm: Int
        get() = prefs.getInt(KEY_RADIUS_KM, 5)
        set(value) = prefs.edit().putInt(KEY_RADIUS_KM, value).apply()

    // Guardar y obtener el filtro seleccionado (0=Todos, 1=Cultivos, 2=Procesamiento, 3=Almacenamiento)
    var filterType: Int
        get() = prefs.getInt(KEY_FILTER_TYPE, 0)
        set(value) = prefs.edit().putInt(KEY_FILTER_TYPE, value).apply()
}
