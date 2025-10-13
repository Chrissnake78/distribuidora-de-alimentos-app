package com.christian.distribuidoradealimentosapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.christian.distribuidoradealimentosapp.R
import com.christian.distribuidoradealimentosapp.data.FirebaseRepo
import com.christian.distribuidoradealimentosapp.data.Product
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.database.*
import kotlin.math.*

class CartActivity : AppCompatActivity() {

    private lateinit var txtResumenCarrito: TextView
    private lateinit var btnCalcularDespacho: Button
    private lateinit var txtResultadoDespacho: TextView
    private lateinit var db: DatabaseReference
    private val carrito = mutableListOf<Product>()
    private var totalCarrito = 0.0

    // Ubicación
    private val REQ_LOC_CART = 2001
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    // ===== CONFIGURACIÓN =====
    private companion object {
        // Plaza de Armas de San Felipe (aprox.)
        private const val LAT_ORIGEN = -32.7485
        private const val LON_ORIGEN = -70.7257
        // En pruebas, permite calcular costo aunque la distancia sea >20 km
        private const val MODO_PRUEBAS_IGNORAR_RADIO = false
    }
    // =========================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        txtResumenCarrito = findViewById(R.id.txtResumenCarrito)
        btnCalcularDespacho = findViewById(R.id.btnCalcularDespacho)
        txtResultadoDespacho = findViewById(R.id.txtResultadoDespacho)

        // Si tu layout tiene el botón, lo tomamos; si no, no pasa nada (no crashea).
        findViewById<Button?>(R.id.btnLimpiarCarrito)?.setOnClickListener {
            FirebaseRepo.carritoRef().removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    carrito.clear()
                    totalCarrito = 0.0
                    txtResumenCarrito.text = "Carrito vacío"
                    txtResultadoDespacho.text = ""
                    Toast.makeText(this, "Carrito vaciado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No se pudo vaciar. Revisa la conexión.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        db = FirebaseRepo.carritoRef()

        // Cargar productos del carrito desde Firebase
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                carrito.clear()
                totalCarrito = 0.0
                val resumen = StringBuilder("Productos en carrito:\n\n")

                for (item in snapshot.children) {
                    val producto = item.getValue(Product::class.java)
                    if (producto != null) {
                        carrito.add(producto)
                        val subtotal = producto.precio * producto.cantidad
                        resumen.append("- ${producto.nombre} x${producto.cantidad} -> $${subtotal.toInt()}\n")
                        totalCarrito += subtotal
                    }
                }

                resumen.append("\nTotal carrito: $${totalCarrito.toInt()} CLP")
                txtResumenCarrito.text = resumen.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Calcular despacho (ubicación + reglas)
        btnCalcularDespacho.setOnClickListener {
            if (!hasLocationPermission()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQ_LOC_CART
                )
                return@setOnClickListener
            }

            if (!isLocationEnabled()) {
                txtResultadoDespacho.text = "Activa la ubicación (GPS) y vuelve a intentar."
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                return@setOnClickListener
            }

            obtainSmartLocation { loc ->
                if (loc == null) {
                    txtResultadoDespacho.text = "No fue posible obtener la ubicación. Intenta cerca de una ventana y verifica permisos."
                    return@obtainSmartLocation
                }

                val distanciaKm = haversineKm(loc.latitude, loc.longitude, LAT_ORIGEN, LON_ORIGEN)
                val (costo, fueraRadio) = calcularDespachoConReglas(distanciaKm, totalCarrito)

                if (costo < 0) {
                    txtResultadoDespacho.text = """
                        Distancia: ${"%.2f".format(distanciaKm)} km
                        Fuera de radio (más de 20 km).
                    """.trimIndent()
                } else {
                    val totalFinal = totalCarrito + costo
                    val aviso = if (fueraRadio) "\n(Nota: fuera de radio, mostrado solo por modo de pruebas)" else ""
                    txtResultadoDespacho.text = """
                        Distancia: ${"%.2f".format(distanciaKm)} km
                        Costo de despacho: $costo CLP
                        Total con despacho: ${totalFinal.toInt()} CLP$aviso
                    """.trimIndent()
                }
            }
        }
    }

    // Permisos / estado de ubicación
    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun isLocationEnabled(): Boolean {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // Ubicación con fallback (Fused → LocationManager)
    private fun obtainSmartLocation(onResult: (Location?) -> Unit) {
        tryFusedFirst { fusedLoc ->
            if (fusedLoc != null) onResult(fusedLoc)
            else tryLocationManager(onResult)
        }
    }

    // 1) Intento con Fused
    private fun tryFusedFirst(onResult: (Location?) -> Unit) {
        try {
            val token = CancellationTokenSource()
            fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        onResult(loc)
                    } else {
                        val req = LocationRequest.Builder(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            1000L
                        ).setMaxUpdates(1).build()

                        val cb = object : LocationCallback() {
                            override fun onLocationResult(result: LocationResult) {
                                fused.removeLocationUpdates(this)
                                onResult(result.lastLocation)
                            }
                        }

                        try {
                            fused.requestLocationUpdates(req, cb, Looper.getMainLooper())
                            Handler(Looper.getMainLooper()).postDelayed({
                                fused.removeLocationUpdates(cb)
                                onResult(null)
                            }, 4000L)
                        } catch (_: SecurityException) {
                            onResult(null)
                        } catch (_: Exception) {
                            onResult(null)
                        }
                    }
                }
                .addOnFailureListener { onResult(null) }
        } catch (_: Exception) {
            onResult(null)
        }
    }

    // 2) LocationManager (sirve en Huawei sin Google)
    private fun tryLocationManager(onResult: (Location?) -> Unit) {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager

        bestLastKnown(lm)?.let { onResult(it); return }

        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) { onResult(null); return }

        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                lm.removeUpdates(this)
                onResult(location)
            }
        }

        try {
            lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            Handler(Looper.getMainLooper()).postDelayed({
                try { lm.removeUpdates(listener) } catch (_: Exception) {}
                onResult(null)
            }, 6000L)
        } catch (_: SecurityException) {
            onResult(null)
        } catch (_: Exception) {
            onResult(null)
        }
    }

    private fun bestLastKnown(lm: LocationManager): Location? {
        var best: Location? = null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        for (p in providers) {
            try {
                val l = lm.getLastKnownLocation(p) ?: continue
                if (best == null || l.accuracy < best!!.accuracy) best = l
            } catch (_: SecurityException) {}
        }
        return best
    }

    // Reglas del despacho
    // Devuelve: Pair(costo, fueraDeRadio)
    private fun calcularDespachoConReglas(distanciaKm: Double, montoCarrito: Double): Pair<Int, Boolean> {
        val fuera = distanciaKm > 20.0
        val costoBase = when {
            montoCarrito >= 50_000.0 -> 0.0
            montoCarrito >= 25_000.0 -> distanciaKm * 150.0
            else -> distanciaKm * 300.0
        }.roundToInt()

        return if (fuera && !MODO_PRUEBAS_IGNORAR_RADIO) {
            Pair(-1, true)
        } else {
            Pair(costoBase, fuera)
        }
    }

    // Haversine (ojo: dLon = lon2 - lon1)
    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    // Permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOC_CART &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            btnCalcularDespacho.performClick()
        }
    }
}