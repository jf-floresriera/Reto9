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
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.reto9.data.OverpassResponse
import com.example.reto9.data.RetrofitClient
import com.example.reto9.utils.PreferencesManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var prefsManager: PreferencesManager

    private var currentUserLocation: GeoPoint? = null
    private val locationPermissionCode = 1001

    private val filters = arrayOf("Todos", "Cultivos", "Procesamiento", "Almacenamiento")

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

        setupUI()
        requestLocationPermission()
    }

    private fun setupUI() {
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
                requestLocationPermission()
            }
        }
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

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                locationPermissionCode
            )
        } else {
            getCurrentLocation()
        }
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
        // Limpiar marcadores anteriores excepto el del usuario
        mapView.overlays.retainAll { it is Marker && it.title == "Mi Ubicación" }

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
                    if (elements.isEmpty()) {
                        Toast.makeText(this@MainActivity, "No se encontraron resultados", Toast.LENGTH_SHORT).show()
                    } else {
                        for (element in elements) {
                            val poiMarker = Marker(mapView)
                            poiMarker.position = GeoPoint(element.latitude, element.longitude)
                            poiMarker.title = element.name
                            poiMarker.snippet = "Tipo: ${element.type} - ID: ${element.id}"
                            poiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            
                            // Cambiar color/icono dependiendo de tags si se quisiera, por ahora usamos el default
                            mapView.overlays.add(poiMarker)
                        }
                        mapView.invalidate()
                        Toast.makeText(this@MainActivity, "Encontrados: ${elements.size}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<OverpassResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
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