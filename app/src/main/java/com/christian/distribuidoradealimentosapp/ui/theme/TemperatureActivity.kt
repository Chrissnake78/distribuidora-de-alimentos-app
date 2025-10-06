package com.christian.distribuidoradealimentosapp.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.christian.distribuidoradealimentosapp.R
import com.christian.distribuidoradealimentosapp.data.SettingsDataStore
import com.google.firebase.database.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class TemperatureActivity : AppCompatActivity() {

    private lateinit var txtLectura: TextView
    private lateinit var txtMin: EditText
    private lateinit var txtMax: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnSimular: Button
    private lateinit var database: DatabaseReference
    private lateinit var settings: SettingsDataStore

    // Rango por defecto exigido por la pauta
    private var minC = 2.0
    private var maxC = 8.0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temperature)

        // Referencias UI
        txtLectura = findViewById(R.id.txtLectura)
        txtMin = findViewById(R.id.txtMin)
        txtMax = findViewById(R.id.txtMax)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnSimular = findViewById(R.id.btnSimular)

        // DataStore
        settings = SettingsDataStore(this)

        // Canal y permiso de notificaciones
        createNotificationChannel()
        requestPostNotificationsIfNeeded()

        // Cargar límites guardados (o defaults 2–8)
        lifecycleScope.launch {
            minC = settings.minTemp.first()
            maxC = settings.maxTemp.first()
            txtMin.setText(minC.toString())
            txtMax.setText(maxC.toString())
        }

        // LECTURA DESDE FIREBASE EN FAHRENHEIT (exigencia de pauta)
        // Ruta exacta esperada: sensors/temperatureF (número Double)
        database = FirebaseDatabase.getInstance().getReference("sensors/temperatureF")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempF = snapshot.getValue(Double::class.java) ?: return
                val tempC = (tempF - 32.0) * (5.0 / 9.0) // Conversión °F -> °C
                verificarRangoYMostrar(tempC)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TemperatureActivity, "Error al leer temperatura", Toast.LENGTH_SHORT).show()
            }
        })

        // Guardar nuevos límites en DataStore
        btnGuardar.setOnClickListener {
            val nuevoMin = txtMin.text.toString().toDoubleOrNull()
            val nuevoMax = txtMax.text.toString().toDoubleOrNull()
            if (nuevoMin == null || nuevoMax == null) {
                Toast.makeText(this, "Ingresa valores válidos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                settings.saveMinTemp(nuevoMin)
                settings.saveMaxTemp(nuevoMax)
                minC = nuevoMin
                maxC = nuevoMax
                Toast.makeText(this@TemperatureActivity, "Rangos guardados en el teléfono", Toast.LENGTH_SHORT).show()
            }
        }

        // Simular lecturas remotas en °F (para demo)
        btnSimular.setOnClickListener {
            val tempF = Random.nextDouble(30.0, 70.0) // 30–70 °F
            database.setValue(tempF)
        }
    }

    private fun verificarRangoYMostrar(tempC: Double) {
        val enRango = tempC in minC..maxC

        // Texto claro (corregimos el error de "...emp")
        val texto = buildString {
            append("Temperatura actual: ${"%.1f".format(tempC)} °C\n")
            append("Rango permitido: ${"%.1f".format(minC)} - ${"%.1f".format(maxC)} °C\n")
            append(if (enRango) "✅ Dentro del rango" else "⚠️ ¡Fuera del rango!")
        }
        txtLectura.text = texto

        if (!enRango) lanzarAlerta(tempC)
    }

    private fun lanzarAlerta(tempC: Double) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(this, "alerta_temp")
            .setSmallIcon(android.R.drawable.stat_sys_warning) // Ícono seguro presente en Android
            .setContentTitle("ALERTA DE TEMPERATURA")
            .setContentText("Actual: ${"%.1f".format(tempC)} °C (fuera del rango)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(sonido)
            .setVibrate(longArrayOf(0, 500, 1000, 500))

        manager.notify(1001, builder.build())

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alerta_temp",
                "Alertas de Temperatura",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}