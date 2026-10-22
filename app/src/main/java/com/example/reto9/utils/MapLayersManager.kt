package com.example.reto9.utils

import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource

object MapLayersManager {

    val osmLayer: ITileSource = TileSourceFactory.MAPNIK

    val esriLayer: ITileSource = XYTileSource(
        "EsriWorldImagery",
        0, 19, 256, ".jpg",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
    )

    val usgsTopoLayer: ITileSource = XYTileSource(
        "USGSTopo",
        0, 15, 256, ".png",
        arrayOf("https://basemap.nationalmap.gov/arcgis/rest/services/USGSTopo/MapServer/tile/")
    )

    // Nota: Para Planetary Computer sin token estricto, usaremos un endpoint público que usualmente proporciona
    // Sentinel-2 L2A o similar global. En producción real se usa STAC API para conseguir los tokens.
    // Usaremos un XYZ alternativo robusto de ESRI o MAXAR si falla, pero definiremos una base genérica
    val microsoftPlanetaryLayer: ITileSource = XYTileSource(
        "MicrosoftPlanetary",
        0, 18, 256, ".png",
        arrayOf(
            // Usando Sentinel-2 o similar global libre temporal, a modo demostrativo de Microsoft
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/" // Placeholder de satélite estable si no hay endpoint XYZ libre fijo de MPC
        )
    )

}