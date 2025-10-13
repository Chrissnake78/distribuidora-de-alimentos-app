package com.christian.distribuidoradealimentosapp.data

data class CartItem(
    val id: String = "",
    val nombre: String = "",
    val precio: Double = 0.0,
    var cantidad: Int = 1 // 🔹 nueva propiedad
) {
    // 🔹 Subtotal automático
    fun subtotal(): Double = precio * cantidad
}