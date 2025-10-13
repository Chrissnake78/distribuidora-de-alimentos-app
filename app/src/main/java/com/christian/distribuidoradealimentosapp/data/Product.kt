package com.christian.distribuidoradealimentosapp.data

data class Product(
    var id: String? = null,
    var nombre: String = "",
    var precio: Double = 0.0,
    var requiereFrio: Boolean = false,

    // 🔹 nuevo campo
    var cantidad: Int = 1
)