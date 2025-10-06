package com.christian.distribuidoradealimentosapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseRepo {

    private val root: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference
    }

    private fun uid(): String =
        FirebaseAuth.getInstance().currentUser?.uid ?: "anon"

    /** Catálogo global */
    fun catalogoRef(): DatabaseReference =
        root.child("catalogo")

    /** Carrito del usuario */
    fun carritoRef(): DatabaseReference =
        root.child("carritos").child(uid())

    /** Órdenes del usuario (opcional) */
    fun ordersRef(): DatabaseReference =
        root.child("orders").child(uid())

    /** 🔥 Configuración de temperatura (límite) */
    fun configTempRef(): DatabaseReference =
        root.child("config").child("tempMaxC")     // por ej. 4 (°C)

    /** 🔥 Sensor de temperatura “en vivo” */
    fun sensorTempRef(): DatabaseReference =
        root.child("sensors").child("temperatureC") // por ej. 3.7
}