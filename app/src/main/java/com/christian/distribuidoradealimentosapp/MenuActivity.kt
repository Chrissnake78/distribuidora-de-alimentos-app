package com.christian.distribuidoradealimentosapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.christian.distribuidoradealimentosapp.ui.CatalogActivity
import com.christian.distribuidoradealimentosapp.ui.TemperatureActivity
import com.google.firebase.auth.FirebaseAuth

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // (opcional) muestra el mail logueado
        FirebaseAuth.getInstance().currentUser?.email?.let {
            Toast.makeText(this, "Sesión: $it", Toast.LENGTH_SHORT).show()
        }

        // Abrir Catálogo
        findViewById<Button>(R.id.btnCatalogo).setOnClickListener {
            startActivity(Intent(this, CatalogActivity::class.java))
        }

        // Ir a Carrito/Despacho (prueba en ui y raíz)
        findViewById<Button>(R.id.btnIrCarrito).setOnClickListener {
            try {
                startActivity(
                    Intent(
                        this,
                        Class.forName("com.christian.distribuidoradealimentosapp.ui.CartActivity")
                    )
                )
            } catch (_: ClassNotFoundException) {
                try {
                    startActivity(
                        Intent(
                            this,
                            Class.forName("com.christian.distribuidoradealimentosapp.CartActivity")
                        )
                    )
                } catch (_: ClassNotFoundException) {
                    Toast.makeText(this, "No encuentro CartActivity.", Toast.LENGTH_LONG).show()
                }
            }
        }

        // 👉 Abrir Temperatura (ESTE es el click que preguntas)
        findViewById<Button>(R.id.btnTemp).setOnClickListener {
            startActivity(Intent(this, TemperatureActivity::class.java))
        }

        // Cerrar sesión
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}