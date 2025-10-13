package com.christian.distribuidoradealimentosapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.christian.distribuidoradealimentosapp.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val REQ_LOC = 5001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_map)

        map = findViewById(R.id.map)
        map.setMultiTouchControls(true)
        map.setTileSource(TileSourceFactory.MAPNIK)

        // Centro por defecto: Plaza de Armas de San Felipe
        val plazaSanFelipe = GeoPoint(-32.7485, -70.7257)
        map.controller.setZoom(14.0)
        map.controller.setCenter(plazaSanFelipe)

        // Si ya hay permiso, intentamos centrar en mi ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            centrarEnMiUbicacion()
        } else {
            // Pedimos permiso
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQ_LOC
            )
        }
    }

    private fun centrarEnMiUbicacion() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager

        // 1) Última ubicación conocida (rápido)
        val last = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).firstNotNullOfOrNull { p ->
            try { lm.getLastKnownLocation(p) } catch (_: SecurityException) { null }
        }
        if (last != null) {
            pintarYCentra(last)
            return
        }

        // 2) Si no hay "last", pedimos 1 actualización con timeout
        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        } ?: return

        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                lm.removeUpdates(this)
                pintarYCentra(location)
            }
        }
        try {
            lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            Handler(Looper.getMainLooper()).postDelayed({
                try { lm.removeUpdates(listener) } catch (_: Exception) {}
            }, 6000L)
        } catch (_: Exception) {
            // Si falla, dejamos centrado en San Felipe
        }
    }

    private fun pintarYCentra(loc: Location) {
        val gp = GeoPoint(loc.latitude, loc.longitude)
        map.controller.setCenter(gp)
        val m = Marker(map)
        m.position = gp
        m.title = "Mi ubicación"
        map.overlays.add(m)
        map.invalidate()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOC &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            centrarEnMiUbicacion()
        }
    }
}