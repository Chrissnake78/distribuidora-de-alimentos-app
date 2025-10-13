//package com.christian.distribuidoradealimentosapp.ui

//import android.content.Intent
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import androidx.appcompat.app.AppCompatActivity
//import com.christian.distribuidoradealimentosapp.R
//import com.christian.distribuidoradealimentosapp.ui.MenuActivity
//
//annotation class MenuActivity
//
//class SplashActivity : AppCompatActivity() {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_splash)
//
//        // Esperamos 3 segundos antes de abrir MenuActivity
//        Handler().postDelayed({
//            val intent = Intent(this, MenuActivity::class.java)
//            startActivity(intent)
//            finish() // Evita que el usuario vuelva al Splash si hace retroceder
//        }, 3000) // 3000 milisegundos = 3 segundos
//    }
//}