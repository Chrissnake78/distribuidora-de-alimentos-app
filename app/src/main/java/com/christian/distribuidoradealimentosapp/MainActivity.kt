package com.christian.distribuidoradealimentosapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlin.math.PI

private const val TAG = "DistribuidoraApp" // Para filtrar en Logcat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val entrada = findViewById<EditText>(R.id.entradaGrados)
        val btnCalcular = findViewById<Button>(R.id.btnCalcular)
        val btnErrores = findViewById<Button>(R.id.btnErrores)
        val salidaResultado = findViewById<TextView>(R.id.salidaResultado)
        val salidaMensaje = findViewById<TextView>(R.id.salidaMensaje)

        btnCalcular.setOnClickListener {
            try {
                val grados = entrada.text.toString().replace(",", ".").toDouble()
                val rad = gradosARadianes(grados)

                salidaResultado.text = "Resultado en radianes: $rad"
                salidaMensaje.text = "Grados $grados → Radianes $rad"

                // Salida por defecto (System.out)
                println("RADIANES: $rad")

                // Logcat (Debug)
                Log.d(TAG, "Conversión OK: Grados $grados → Radianes $rad")

            } catch (e: NumberFormatException) {
                salidaResultado.text = "Resultado en radianes: —"
                salidaMensaje.text = "Entrada inválida. Escribir un número decimal."
                Log.e(TAG, "Error de formato en la entrada: '${entrada.text}'", e)
                println("ERROR FORMATO: ${e.message}")
            }
        }

        btnErrores.setOnClickListener {
            generarErroresDeDemostracion()
        }
    }

    private fun gradosARadianes(grados: Double): Double = grados * (PI / 180.0)

    private fun generarErroresDeDemostracion() {
        try { "abc".toDouble() } catch (e: NumberFormatException) {
            Log.e(TAG, "NumberFormatException de demostración", e)
        }
        try { val lista = listOf(1); val x = lista[3] } catch (e: IndexOutOfBoundsException) {
            Log.e(TAG, "IndexOutOfBoundsException de demostración", e)
        }
        try { val s: String? = null; s!!.length } catch (e: NullPointerException) {
            Log.e(TAG, "NullPointerException de demostración", e)
        }
        println("ERRORES DE DEMOSTRACION ENVIADOS A LOGCAT")
    }
}