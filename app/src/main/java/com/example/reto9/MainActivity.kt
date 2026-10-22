package com.example.reto9

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.reto9.data.OverpassResponse
import com.example.reto9.data.RetrofitClient
import com.example.reto9.utils.MapLayersManager
import com.example.reto9.utils.PreferencesManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity(), MapEventsReceiver {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var prefsManager: PreferencesManager

    private var currentUserLocation: GeoPoint? = null
    private val permissionCode = 1001

    private val filters = arrayOf("Todos", "Cultivos", "Procesamiento", "Almacenamiento")

    // Modos de dibujo
    enum class DrawMode { NONE, LINE, POLYGON }
    private var currentDrawMode = DrawMode.NONE
    private val currentDrawPoints = mutableListOf<GeoPoint>()
    
    private var activePolyline: Polyline? = null
    private var activePolygon: Polygon? = null

    // Manejador de cache para offline
    private lateinit var cacheManager: CacheManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar OSMDroid
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        
        setContentView(R.layout.activity_main)

        prefsManager = PreferencesManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mapView = findViewById(R.id.mapView)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(14.0)

        // Inicializar CacheManager
        cacheManager = CacheManager(mapView)

        // Overlay de eventos para detectar toques en el mapa (para dibujo)
        val mapEventsOverlay = MapEventsOverlay(this)
        mapView.overlays.add(0, mapEventsOverlay) // Agregar al fondo

        setupUI()
        requestPermissions()
    }

    private fun setupUI() {
        // Spinner para el filtro de lugares agro (Requisito profesor)
        val spinnerFilter = findViewById<Spinner>(R.id.spinnerFilter)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filters)
        spinnerFilter.adapter = adapter
        spinnerFilter.setSelection(prefsManager.filterType)

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (prefsManager.filterType != position) {
                    prefsManager.filterType = position
                    if (currentUserLocation != null) {
                        fetchPOIs(currentUserLocation!!)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }

        findViewById<FloatingActionButton>(R.id.fabLocation).setOnClickListener {
            if (currentUserLocation != null) {
                mapView.controller.animateTo(currentUserLocation)
            } else {
                requestPermissions()
            }
        }

        // Botones nuevos
        findViewById<FloatingActionButton>(R.id.fabLayers).setOnClickListener {
            showLayersDialog()
        }

        findViewById<FloatingActionButton>(R.id.fabDraw).setOnClickListener {
            showDrawDialog()
        }

        val fabClearDraw = findViewById<FloatingActionButton>(R.id.fabClearDraw)
        fabClearDraw.setOnClickListener {
            clearDrawings()
            fabClearDraw.visibility = View.GONE
            Toast.makeText(this, "Dibujos borrados", Toast.LENGTH_SHORT).show()
        }

        findViewById<FloatingActionButton>(R.id.fabDownload).setOnClickListener {
            downloadCurrentArea()
        }
    }

    private fun showLayersDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_layers, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupLayers)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Aceptar") { _, _ ->
                when (radioGroup.checkedRadioButtonId) {
                    R.id.rbOsm -> mapView.setTileSource(MapLayersManager.osmLayer)
                    R.id.rbEsri -> mapView.setTileSource(MapLayersManager.esriLayer)
                    R.id.rbUsgs -> mapView.setTileSource(MapLayersManager.usgsTopoLayer)
                    R.id.rbMicrosoft -> mapView.setTileSource(MapLayersManager.microsoftPlanetaryLayer)
                }
                mapView.invalidate()
            }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.show()
    }

    private fun showDrawDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_drawing, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupDraw)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Aceptar") { _, _ ->
                currentDrawMode = when (radioGroup.checkedRadioButtonId) {
                    R.id.rbDrawNone -> DrawMode.NONE
                    R.id.rbDrawLine -> DrawMode.LINE
                    R.id.rbDrawPolygon -> DrawMode.POLYGON
                    else -> DrawMode.NONE
                }
                
                // Limpiar trazo activo pero no necesariamente el historial de trazos
                currentDrawPoints.clear()
                activePolyline = null
                activePolygon = null
                
                val msg = if (currentDrawMode != DrawMode.NONE) "Toca el mapa para dibujar" else "Modo navegación"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.show()
    }

    private fun downloadCurrentArea() {
        val boundingBox = mapView.boundingBox
        val zoomMin = mapView.zoomLevelDouble.toInt()
        val zoomMax = zoomMin + 2 // Descargamos 2 niveles de zoom extra para no saturar

        Toast.makeText(this, "Calculando descarga...", Toast.LENGTH_SHORT).show()

        // Ejecutar descarga en background
        cacheManager.downloadAreaAsync(this, boundingBox, zoomMin, zoomMax)
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val seekBarRadius = dialogView.findViewById<SeekBar>(R.id.seekBarRadius)
        val tvRadiusValue = dialogView.findViewById<TextView>(R.id.tvRadiusValue)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveSettings)

        seekBarRadius.progress = prefsManager.radiusKm
        tvRadiusValue.text = "Radio: ${prefsManager.radiusKm} km"

        seekBarRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actualProgress = if (progress == 0) 1 else progress
                tvRadiusValue.text = "Radio: $actualProgress km"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnSave.setOnClickListener {
            val finalRadius = if (seekBarRadius.progress == 0) 1 else seekBarRadius.progress
            prefsManager.radiusKm = finalRadius
            dialog.dismiss()
            if (currentUserLocation != null) {
                fetchPOIs(currentUserLocation!!)
            }
        }
        dialog.show()
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        ActivityCompat.requestPermissions(this, permissions, permissionCode)
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val userPoint = GeoPoint(location.latitude, location.longitude)
                currentUserLocation = userPoint
                
                mapView.controller.setCenter(userPoint)

                // Marcador del usuario
                val userMarker = Marker(mapView)
                userMarker.position = userPoint
                userMarker.title = "Mi Ubicación"
                userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                
                mapView.overlays.add(userMarker)
                mapView.invalidate()

                fetchPOIs(userPoint)
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchPOIs(centerPoint: GeoPoint) {
        // Retener mapa base de eventos y marcadores manuales/dibujos (Polygon/Polyline)
        mapView.overlays.retainAll { 
            it is MapEventsOverlay || 
            it is Polygon || 
            it is Polyline || 
            (it is Marker && it.title == "Mi Ubicación") 
        }

        val radiusMeters = prefsManager.radiusKm * 1000
        val lat = centerPoint.latitude
        val lon = centerPoint.longitude

        val filterQuery = when (prefsManager.filterType) {
            1 -> """
                node["landuse"="farmland"](around:$radiusMeters,$lat,$lon);
                way["landuse"="farmland"](around:$radiusMeters,$lat,$lon);
                node["landuse"="orchard"](around:$radiusMeters,$lat,$lon);
                way["landuse"="orchard"](around:$radiusMeters,$lat,$lon);
            """.trimIndent()
            2 -> """
                node["industrial"="food"](around:$radiusMeters,$lat,$lon);
                way["industrial"="food"](around:$radiusMeters,$lat,$lon);
                node["industrial"="agricultural"](around:$radiusMeters,$lat,$lon);
                way["industrial"="agricultural"](around:$radiusMeters,$lat,$lon);
            """.trimIndent()
            3 -> """
                node["man_made"="silo"](around:$radiusMeters,$lat,$lon);
                way["man_made"="silo"](around:$radiusMeters,$lat,$lon);
                node["building"="warehouse"](around:$radiusMeters,$lat,$lon);
                way["building"="warehouse"](around:$radiusMeters,$lat,$lon);
            """.trimIndent()
            else -> """
                node["landuse"="farmland"](around:$radiusMeters,$lat,$lon);
                way["landuse"="farmland"](around:$radiusMeters,$lat,$lon);
                node["industrial"="food"](around:$radiusMeters,$lat,$lon);
                way["industrial"="food"](around:$radiusMeters,$lat,$lon);
                node["man_made"="silo"](around:$radiusMeters,$lat,$lon);
                way["man_made"="silo"](around:$radiusMeters,$lat,$lon);
            """.trimIndent()
        }

        val overpassQuery = """
            [out:json];
            (
              $filterQuery
            );
            out center;
        """.trimIndent()

        RetrofitClient.instance.getPoi(overpassQuery).enqueue(object : Callback<OverpassResponse> {
            override fun onResponse(call: Call<OverpassResponse>, response: Response<OverpassResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val elements = response.body()!!.elements
                    if (elements.isNotEmpty()) {
                        for (element in elements) {
                            val poiMarker = Marker(mapView)
                            poiMarker.position = GeoPoint(element.latitude, element.longitude)
                            poiMarker.title = element.name
                            poiMarker.snippet = "Tipo: ${element.type} - ID: ${element.id}"
                            poiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            
                            mapView.overlays.add(poiMarker)
                        }
                        mapView.invalidate()
                        Toast.makeText(this@MainActivity, "Encontrados: ${elements.size} lugares", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<OverpassResponse>, t: Throwable) {}
        })
    }

    // --- MÉTODOS DE DIBUJO ---
    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        if (p == null || currentDrawMode == DrawMode.NONE) return false

        findViewById<FloatingActionButton>(R.id.fabClearDraw).visibility = View.VISIBLE
        currentDrawPoints.add(p)

        if (currentDrawMode == DrawMode.LINE) {
            if (activePolyline == null) {
                activePolyline = Polyline(mapView)
                activePolyline!!.outlinePaint.color = android.graphics.Color.BLUE
                activePolyline!!.outlinePaint.strokeWidth = 5f
                mapView.overlays.add(activePolyline)
            }
            activePolyline!!.setPoints(currentDrawPoints)
        } 
        else if (currentDrawMode == DrawMode.POLYGON) {
            if (activePolygon == null) {
                activePolygon = Polygon(mapView)
                activePolygon!!.fillPaint.color = android.graphics.Color.argb(75, 255, 0, 0)
                activePolygon!!.outlinePaint.color = android.graphics.Color.RED
                activePolygon!!.outlinePaint.strokeWidth = 5f
                mapView.overlays.add(activePolygon)
            }
            activePolygon!!.points = currentDrawPoints
            
            // Si hay 3 o más puntos, podemos calcular un área simple
            if (currentDrawPoints.size >= 3) {
                Toast.makeText(this, "Polígono con ${currentDrawPoints.size} vértices", Toast.LENGTH_SHORT).show()
            }
        }

        mapView.invalidate()
        return true
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        return false // Ignorar pulsación larga
    }

    private fun clearDrawings() {
        mapView.overlays.removeAll { it is Polygon || it is Polyline }
        currentDrawPoints.clear()
        activePolyline = null
        activePolygon = null
        mapView.invalidate()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionCode) {
            // Asumimos permisos principales, pero es ideal verificar
            getCurrentLocation()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}