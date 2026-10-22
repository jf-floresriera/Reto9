package com.example.reto9.data

import com.google.gson.annotations.SerializedName

data class OverpassResponse(
    @SerializedName("elements")
    val elements: List<OverpassElement>
)

data class OverpassElement(
    @SerializedName("type")
    val type: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("lat")
    val lat: Double?,
    @SerializedName("lon")
    val lon: Double?,
    @SerializedName("center")
    val center: Center?,
    @SerializedName("tags")
    val tags: Map<String, String>?
) {
    // Overpass puede retornar lat/lon directo (para nodos) o en 'center' (para ways/relations)
    val latitude: Double
        get() = lat ?: center?.lat ?: 0.0

    val longitude: Double
        get() = lon ?: center?.lon ?: 0.0

    val name: String
        get() = tags?.get("name") ?: "Desconocido"
}

data class Center(
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lon")
    val lon: Double
)
