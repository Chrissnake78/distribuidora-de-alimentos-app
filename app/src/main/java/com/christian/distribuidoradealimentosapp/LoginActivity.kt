package com.christian.distribuidoradealimentosapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPass  = findViewById<EditText>(R.id.etPass)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnIrRegistro = findViewById<Button>(R.id.btnIrRegistro) // 👈 lo agregamos aquí

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass  = etPass.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa email y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Intentando login…", Toast.LENGTH_SHORT).show()

            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login OK", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MenuActivity::class.java))
                    finish()
                } else {
                    val ex = task.exception
                    when (ex) {
                        is FirebaseAuthInvalidUserException -> {
                            auth.createUserWithEmailAndPassword(email, pass)
                                .addOnCompleteListener { ct ->
                                    if (ct.isSuccessful) {
                                        Toast.makeText(this, "Usuario creado. Entrando…", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, MenuActivity::class.java))
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Crear usuario falló: ${ct.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(this, "Credenciales inválidas: ${ex.message}", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(this, "Login fallido: ${ex?.message ?: "Error desconocido"}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        btnIrRegistro.setOnClickListener {
            val intent = Intent(this, com.christian.distribuidoradealimentosapp.ui.RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}