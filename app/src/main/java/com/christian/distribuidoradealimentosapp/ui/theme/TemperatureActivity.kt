package com.christian.distribuidoradealimentosapp.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.christian.distribuidoradealimentosapp.R
import com.christian.distribuidoradealimentosapp.data.SettingsDataStore
import com.google.firebase.database.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TemperatureActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var txtLectura: TextView
    private lateinit var txtMin: EditText
    private lateinit var txtMax: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnSilenciar: Button

    private lateinit var settings: SettingsDataStore
    private var minC = 2.0
    private var maxC = 8.0
    private var mediaPlayer: MediaPlayer? = null

    private val CHANNEL_ID = "alertasTemp"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temperature)

        // Views del XML (VERTICAL y LAND llevan los mismos IDs)
        txtLectura = findViewById(R.id.txtLectura)
        txtMin = findViewById(R.id.txtMin)
        txtMax = findViewById(R.id.txtMax)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnSilenciar = findViewById(R.id.btnSilenciar)

        settings = SettingsDataStore(this)

        // Carga rangos guardados
        lifecycleScope.launch {
            minC = settings.minTemp.first()
            maxC = settings.maxTemp.first()
            txtMin.setText(minC.toString())
            txtMax.setText(maxC.toString())
        }

        pedirPermisoNotificaciones()
        crearCanal()

        database = FirebaseDatabase.getInstance().getReference("sensors/temperatureF")

        // Lectura en tiempo real
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempF = snapshot.getValue(Double::class.java) ?: return
                val tempC = (tempF - 32.0) * (5.0 / 9.0)
                txtLectura.text = "Temperatura actual: %.1f °C".format(tempC)

                if (tempC < minC || tempC > maxC) {
                    alarma()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TemperatureActivity,
                    "Error al leer temperatura: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Guardar nuevos rangos
        btnGuardar.setOnClickListener {
            val nuevoMin = txtMin.text.toString().toDoubleOrNull()
            val nuevoMax = txtMax.text.toString().toDoubleOrNull()
            if (nuevoMin == null || nuevoMax == null || nuevoMin >= nuevoMax) {
                Toast.makeText(this, "Valores no válidos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                settings.saveMinTemp(nuevoMin)
                settings.saveMaxTemp(nuevoMax)
                minC = nuevoMin
                maxC = nuevoMax
                Toast.makeText(this@TemperatureActivity, "Rangos guardados", Toast.LENGTH_SHORT).show()
            }
        }

        // Silenciar manual
        btnSilenciar.setOnClickListener { detenerAlarma() }
    }

    private fun alarma() {
        // Vibrar
        val vib = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(800)
        }

        // Sonido propio (raw/alerta.mp3), en bucle hasta silenciar
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alerta).apply {
                isLooping = true
                start()
            }
        }

        // Notificación visual
        mostrarNotificacion()
    }

    private fun detenerAlarma() {
        mediaPlayer?.run {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        Toast.makeText(this, "Alarma silenciada", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarNotificacion() {
        val intent = Intent(this, TemperatureActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠️ Alerta de Temperatura")
            .setContentText("Temperatura fuera del rango establecido")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ActivityCompat.checkSelfPermission(
                    this@TemperatureActivity, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(1001, builder.build())
            }
        }
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID, "Alertas de Temperatura",
                NotificationManager.IMPORTANCE_HIGH
            )
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(canal)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerAlarma()
    }
}