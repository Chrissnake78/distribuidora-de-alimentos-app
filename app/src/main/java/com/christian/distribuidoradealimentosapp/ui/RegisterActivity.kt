package com.christian.distribuidoradealimentosapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.christian.distribuidoradealimentosapp.R
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val txtEmail: EditText = findViewById(R.id.txtEmail)
        val txtPass: EditText = findViewById(R.id.txtPass)
        val btnCrear: Button = findViewById(R.id.btnCrear)
        val btnVolverLogin: Button = findViewById(R.id.btnVolverLogin)

        // Crear usuario
        btnCrear.setOnClickListener {
            val email = txtEmail.text.toString().trim()
            val pass = txtPass.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa ambos campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    Toast.makeText(this, "Usuario creado con éxito", Toast.LENGTH_SHORT).show()
                    finish() // vuelve al Login
                }
                .addOnFailureListener {
                    Toast.makeText(this, it.message ?: "Error al registrar", Toast.LENGTH_SHORT).show()
                }
        }

        // Volver al login
        btnVolverLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}