package com.christian.distribuidoradealimentosapp.data

data class Product(
    var id: String? = null,
    var nombre: String = "",
    var precio: Int = 0,
    var requiereFrio: Boolean = false
)