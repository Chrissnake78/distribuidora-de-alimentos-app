package com.christian.distribuidoradealimentosapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.christian.distribuidoradealimentosapp.ui.CatalogActivity
import com.christian.distribuidoradealimentosapp.ui.CartActivity
import com.christian.distribuidoradealimentosapp.ui.LoginActivity
import com.christian.distribuidoradealimentosapp.ui.TemperatureActivity
import com.google.firebase.auth.FirebaseAuth

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        FirebaseAuth.getInstance().currentUser?.email?.let {
            Toast.makeText(this, "Sesión: $it", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnCatalogo).setOnClickListener {
            startActivity(Intent(this, CatalogActivity::class.java))
        }

        findViewById<Button>(R.id.btnIrCarrito).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        findViewById<Button>(R.id.btnTemp).setOnClickListener {
            startActivity(Intent(this, TemperatureActivity::class.java))
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val btnMapa = findViewById<Button>(R.id.btnMapa)
        btnMapa.setOnClickListener {
            startActivity(Intent(this, com.christian.distribuidoradealimentosapp.ui.MapActivity::class.java))
        }
    }
}