package com.example.reto9

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.example.reto9.data.OverpassResponse
import com.example.reto9.data.RetrofitClient
import com.example.reto9.utils.MapLayersManager
import com.example.reto9.utils.PreferencesManager
import com.example.reto9.utils.ThemeManager
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity(), MapEventsReceiver {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var prefsManager: PreferencesManager

    private var currentUserLocation: GeoPoint? = null
    private val permissionCode = 1001

    // Categorías de puntos de interés que pide la tarea.
    private val filters = arrayOf(
        "Todos", "Hospitales", "Turístico", "Restaurantes", "Farmacias", "Escuelas"
    )

    // Modos de dibujo
    enum class DrawMode { NONE, LINE, POLYGON }
    private var currentDrawMode = DrawMode.NONE
    private val currentDrawPoints = mutableListOf<GeoPoint>()
    
    private var activePolyline: Polyline? = null
    private var activePolygon: Polygon? = null

    // Manejador de cache para offline
    private lateinit var cacheManager: CacheManager

    // Overlay que dibuja el punto azul de "mi ubicación" real y la sigue.
    private var myLocationOverlay: MyLocationNewOverlay? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar el tema guardado (claro/oscuro/sistema) ANTES de crear la vista.
        prefsManager = PreferencesManager(this)
        ThemeManager.apply(prefsManager.themeMode)

        super.onCreate(savedInstanceState)
        
        // Configurar OSMDroid.
        // IMPORTANTE: hay que asignar un User-Agent ANTES de usar el MapView. Sin esto los
        // servidores de teselas de OpenStreetMap rechazan las peticiones (HTTP 403) y el mapa
        // aparece gris/en blanco. Esta era la causa de que "los mapas no se vieran".
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        // User-Agent único y descriptivo. OSM BLOQUEA (403) los que contienen "com.example".
        // Por eso usamos el applicationId propio (io.github.jfloresriera.reto9) via BuildConfig.
        Configuration.getInstance().userAgentValue = "Reto9/1.0 (${BuildConfig.APPLICATION_ID})"
        // Usar el almacenamiento interno de la app para la caché de teselas. Evita que el mapa
        // salga gris por problemas de permisos de almacenamiento en algunos dispositivos.
        Configuration.getInstance().osmdroidBasePath = cacheDir
        Configuration.getInstance().osmdroidTileCache = java.io.File(cacheDir, "tiles")

        setContentView(R.layout.activity_main)

        // Toolbar Material como barra de acciones (con menú).
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        // prefsManager ya se inicializó al principio de onCreate (para el tema).
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mapView = findViewById(R.id.mapView)
        // Fuente de teselas explícita de OpenStreetMap (si no se define, el mapa sale vacío).
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setUseDataConnection(true)
        mapView.controller.setZoom(14.0)
        // Centro inicial válido para que el mapa NO arranque en la coordenada (0,0)
        // (que cae en el océano) mientras se obtiene la ubicación real del GPS.
        // Plaza de Bolívar, Bogotá, Colombia.
        mapView.controller.setCenter(GeoPoint(4.5981, -74.0758)) // Bogotá, Colombia

        // Inicializar CacheManager
        cacheManager = CacheManager(mapView)

        // Overlay de eventos para detectar toques en el mapa (para dibujo)
        val mapEventsOverlay = MapEventsOverlay(this)
        mapView.overlays.add(0, mapEventsOverlay) // Agregar al fondo

        setupUI()

        // Si ya tenemos permiso, obtenemos la ubicación directamente; si no, lo solicitamos.
        if (hasLocationPermission()) {
            getCurrentLocation()
        } else {
            requestPermissions()
        }
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

        findViewById<FloatingActionButton>(R.id.fabLocation).setOnClickListener {
            if (currentUserLocation != null) {
                mapView.controller.setZoom(15.0)
                mapView.controller.animateTo(currentUserLocation)
            }
            // Siempre vuelve a pedir una lectura fresca del GPS al tocar el botón.
            getCurrentLocation()
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
        val slider = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.sliderRadius)
        val tvRadiusValue = dialogView.findViewById<TextView>(R.id.tvRadiusValue)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveSettings)

        slider.value = prefsManager.radiusKm.toFloat().coerceIn(1f, 50f)
        tvRadiusValue.text = "Radio: ${prefsManager.radiusKm} km"

        slider.addOnChangeListener { _, value, _ ->
            tvRadiusValue.text = "Radio: ${value.toInt()} km"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnSave.setOnClickListener {
            prefsManager.radiusKm = slider.value.toInt()
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

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun isGpsEnabled(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Activa el overlay azul de "mi ubicación" de osmdroid, que lee el GPS directamente
     * y sigue la posición real del usuario sobre el mapa.
     */
    @SuppressLint("MissingPermission")
    private fun enableMyLocationOverlay() {
        if (myLocationOverlay == null) {
            val overlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
            overlay.enableMyLocation()
            myLocationOverlay = overlay
            mapView.overlays.add(overlay)
        }
    }

    /**
     * Obtiene la ubicación REAL y fresca del GPS.
     *
     * Antes el código solo usaba lastLocation, que devuelve la última posición en caché
     * (vieja, de otro lugar, o la ubicación por defecto del emulador = "ubicación ficticia").
     * Ahora se pide una lectura nueva con getCurrentLocation() y solo se usa lastLocation
     * como respaldo si la lectura fresca no llega.
     */
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (!hasLocationPermission()) {
            requestPermissions()
            return
        }

        if (!isGpsEnabled()) {
            Toast.makeText(
                this,
                "Activa la ubicación (GPS) de tu dispositivo para detectar tu posición.",
                Toast.LENGTH_LONG
            ).show()
        }

        enableMyLocationOverlay()
        Toast.makeText(this, "Obteniendo tu ubicación...", Toast.LENGTH_SHORT).show()

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0) // 0 = fuerza una lectura nueva, no usa caché
            .build()

        fusedLocationClient.getCurrentLocation(request, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onLocationReady(location)
                } else {
                    // Respaldo: intentar con la última ubicación conocida.
                    fusedLocationClient.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            onLocationReady(last)
                        } else {
                            Toast.makeText(
                                this,
                                "No se pudo obtener tu ubicación. Verifica que el GPS esté activo y con señal.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Error al obtener la ubicación: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun onLocationReady(location: Location) {
        val userPoint = GeoPoint(location.latitude, location.longitude)
        currentUserLocation = userPoint

        mapView.controller.setZoom(15.0)
        mapView.controller.animateTo(userPoint)
        mapView.invalidate()

        fetchPOIs(userPoint)
    }

    private fun fetchPOIs(centerPoint: GeoPoint) {
        // Conservar: overlay de eventos (toques), dibujos (Polygon/Polyline) y el overlay azul
        // de "mi ubicación". Se eliminan solo los marcadores de POIs de la búsqueda anterior.
        mapView.overlays.retainAll {
            it is MapEventsOverlay ||
            it is Polygon ||
            it is Polyline ||
            it is MyLocationNewOverlay
        }

        val radiusMeters = prefsManager.radiusKm * 1000
        val lat = centerPoint.latitude
        val lon = centerPoint.longitude

        // Construye el filtro Overpass según la categoría elegida. Se consultan nodes, ways y
        // relations para una determinada etiqueta OSM (key=value) dentro del radio.
        fun block(key: String, value: String): String = """
            node["$key"="$value"](around:$radiusMeters,$lat,$lon);
            way["$key"="$value"](around:$radiusMeters,$lat,$lon);
            relation["$key"="$value"](around:$radiusMeters,$lat,$lon);
        """.trimIndent()

        val filterQuery = when (prefsManager.filterType) {
            1 -> block("amenity", "hospital")
            2 -> block("tourism", "attraction")
            3 -> block("amenity", "restaurant")
            4 -> block("amenity", "pharmacy")
            5 -> block("amenity", "school")
            else -> listOf(
                block("amenity", "hospital"),
                block("tourism", "attraction"),
                block("amenity", "restaurant"),
                block("amenity", "pharmacy"),
                block("amenity", "school")
            ).joinToString("\n")
        }

        val overpassQuery = """
            [out:json][timeout:40];
            (
              $filterQuery
            );
            out center;
        """.trimIndent()

        Toast.makeText(this, "Buscando puntos de interés...", Toast.LENGTH_SHORT).show()

        RetrofitClient.instance.getPoi(overpassQuery).enqueue(object : Callback<OverpassResponse> {
            override fun onResponse(call: Call<OverpassResponse>, response: Response<OverpassResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val elements = response.body()!!.elements
                    if (elements.isNotEmpty()) {
                        for (element in elements) {
                            // Saltar elementos sin coordenadas válidas.
                            if (element.latitude == 0.0 && element.longitude == 0.0) continue
                            val poiMarker = Marker(mapView)
                            poiMarker.position = GeoPoint(element.latitude, element.longitude)
                            poiMarker.title = element.name   // Nombre del punto de interés (requisito)
                            poiMarker.snippet = "Tipo: ${element.type}"
                            poiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            mapView.overlays.add(poiMarker)
                        }
                        mapView.invalidate()
                        Toast.makeText(
                            this@MainActivity,
                            "Encontrados: ${elements.size} lugares",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "No se encontraron puntos de interés en el radio configurado.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Error del servidor Overpass (${response.code()}).",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<OverpassResponse>, t: Throwable) {
                Toast.makeText(
                    this@MainActivity,
                    "Error de red al buscar puntos de interés: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
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
            val fineGranted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (fineGranted) {
                getCurrentLocation()
            } else {
                Toast.makeText(
                    this,
                    "Se requiere permiso de ubicación para mostrar el mapa y los puntos de interés.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Diálogo para elegir el tema (claro / oscuro / automático). Guarda la elección
     * y la aplica inmediatamente recreando la actividad.
     */
    private fun showThemeDialog() {
        val opciones = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.theme_dialog_title)
            .setSingleChoiceItems(opciones, prefsManager.themeMode) { dialog, which ->
                prefsManager.themeMode = which
                ThemeManager.apply(which)
                dialog.dismiss()
                recreate() // aplica el tema al instante
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                if (currentUserLocation != null) fetchPOIs(currentUserLocation!!)
                else getCurrentLocation()
                true
            }
            R.id.action_layers -> { showLayersDialog(); true }
            R.id.action_download -> { downloadCurrentArea(); true }
            R.id.action_draw -> { showDrawDialog(); true }
            R.id.action_theme -> { showThemeDialog(); true }
            R.id.action_settings -> { showSettingsDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (hasLocationPermission()) {
            myLocationOverlay?.enableMyLocation()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        myLocationOverlay?.disableMyLocation()
    }
}