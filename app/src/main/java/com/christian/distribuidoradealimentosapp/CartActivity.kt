package com.christian.distribuidoradealimentosapp.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.christian.distribuidoradealimentosapp.R
import com.christian.distribuidoradealimentosapp.data.CartItem
import com.christian.distribuidoradealimentosapp.data.FirebaseRepo
import com.christian.distribuidoradealimentosapp.domain.DispatchRules
import com.christian.distribuidoradealimentosapp.LAT_ORIGEN
import com.christian.distribuidoradealimentosapp.LON_ORIGEN
import com.christian.distribuidoradealimentosapp.haversineKm
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*

class CartActivity : AppCompatActivity() {

    private lateinit var txtResumen: TextView
    private lateinit var btnCalcular: Button
    private lateinit var txtResultado: TextView
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private var items: List<CartItem> = emptyList()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) calcularDespachoConPermiso()
        else txtResultado.text = "Permiso de ubicación denegado."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        txtResumen = findViewById(R.id.txtResumenCarrito)
        btnCalcular = findViewById(R.id.btnCalcularDespacho)
        txtResultado = findViewById(R.id.txtResultadoDespacho)

        // Cargar carrito desde Firebase
        FirebaseRepo.carritoRef().addValueEventListener(object: ValueEventListener{
            override fun onDataChange(s: DataSnapshot) {
                val temp = mutableListOf<CartItem>()
                s.children.forEach { c ->
                    c.getValue(CartItem::class.java)?.let { temp.add(it) }
                }
                items = temp
                val total = items.sumOf { it.subtotal }
                val requiereFrio = items.any { it.nombre.contains("congelado", ignoreCase = true) }
                txtResumen.text = "Items: ${items.size}\nTotal compra: $$total\nRequiere frío: $requiereFrio"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        btnCalcular.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED -> calcularDespachoConPermiso()
                else -> permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun calcularDespachoConPermiso() {
        // 1) ¿Hay Google Play Services disponible?
        val gmsCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (gmsCode != ConnectionResult.SUCCESS) {
            // Mensaje claro para el usuario
            val msg = when (gmsCode) {
                ConnectionResult.SERVICE_MISSING -> "Google Play Services no está instalado."
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "Actualiza Google Play Services desde Play Store."
                ConnectionResult.SERVICE_DISABLED -> "Google Play Services está deshabilitado."
                ConnectionResult.SERVICE_INVALID -> "Este dispositivo no tiene servicios de Google válidos (GMS)."
                else -> "Servicios de Google no disponibles (código $gmsCode)."
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

            // 2) Fallback con LocationManager (GPS/Red)
            obtenerUbicacionConLocationManager()?.let { loc ->
                calcularConUbicacion(loc)
            } ?: run {
                txtResultado.text = "No se pudo obtener ubicación. $msg"
            }
            return
        }

        // 3) Caso normal con FusedLocationProviderClient
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            txtResultado.text = "Sin permiso de ubicación."
            return
        }

        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                calcularConUbicacion(loc)
            } else {
                // Si Fused no da nada, intentamos fallback igualmente
                obtenerUbicacionConLocationManager()?.let { l ->
                    calcularConUbicacion(l)
                } ?: run {
                    txtResultado.text = "No se pudo obtener ubicación (Fused y fallback fallaron)."
                }
            }
        }.addOnFailureListener {
            txtResultado.text = "Error de ubicación: ${it.message}"
        }
    }

    private fun obtenerUbicacionConLocationManager(): Location? {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val okFine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val okCoarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!okFine && !okCoarse) return null

        val gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (gps != null) return gps
        val net = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        return net
    }

    private fun calcularConUbicacion(loc: Location) {
        val km = haversineKm(LAT_ORIGEN, LON_ORIGEN, loc.latitude, loc.longitude)
        val total = items.sumOf { it.subtotal }
        val costo = DispatchRules.costoDespacho(total, km)
        val totalFinal = total + costo
        txtResultado.text = "Distancia: ${"%.1f".format(km)} km\nCosto despacho: $$costo\nTotal a pagar: $$totalFinal"
    }
}