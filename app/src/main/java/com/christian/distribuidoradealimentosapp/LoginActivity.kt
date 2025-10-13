package com.christian.distribuidoradealimentosapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.christian.distribuidoradealimentosapp.MenuActivity
import com.christian.distribuidoradealimentosapp.R
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnIrRegistro: Button

    // === NUEVO: ubicación ===
    private val REQ_LOC = 1001
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPass = findViewById(R.id.etPass)
        btnLogin = findViewById(R.id.btnLogin)
        btnIrRegistro = findViewById(R.id.btnIrRegistro)

        // Iniciar sesión
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa ambos campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    Toast.makeText(this, "Inicio de sesión correcto", Toast.LENGTH_SHORT).show()
                    // Guardar ubicación y luego ir al menú
                    afterLoginSaveLocationThenGoMenu()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Ir a registro
        btnIrRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // === NUEVO: flujo de guardar ubicación ===
    private fun afterLoginSaveLocationThenGoMenu() {
        // Si no hay permiso, lo pedimos y seguimos el flujo en onRequestPermissionsResult
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQ_LOC
            )
            return
        }

        // Con permiso concedido, intentamos leer última ubicación y guardarla
        try {
            fused.lastLocation
                .addOnSuccessListener { loc ->
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid == null) {
                        goMenu()
                        return@addOnSuccessListener
                    }

                    val ref = FirebaseDatabase.getInstance()
                        .getReference("locations")
                        .child(uid)

                    val data = mapOf(
                        "lat" to (loc?.latitude ?: 0.0),
                        "lng" to (loc?.longitude ?: 0.0),
                        "ts" to ServerValue.TIMESTAMP
                    )

                    ref.setValue(data)
                        .addOnCompleteListener { goMenu() }   // Pase lo que pase, seguimos al menú
                        .addOnFailureListener { goMenu() }
                }
                .addOnFailureListener {
                    goMenu()
                }
        } catch (_: SecurityException) {
            goMenu()
        }
    }

    private fun goMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
        finish()
    }

    // === NUEVO: respuesta al diálogo de permisos de ubicación ===
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOC) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Usuario aceptó: guardamos ubicación y vamos al menú
                afterLoginSaveLocationThenGoMenu()
            } else {
                // Sin permiso: igual continuamos al menú para no bloquear el uso
                goMenu()
            }
        }
    }
}