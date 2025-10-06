package com.christian.distribuidoradealimentosapp.data

data class CartItem(
    var id: String = "",
    var nombre: String = "",
    var cantidad: Int = 0,
    var precio: Int = 0,
    var subtotal: Int = 0
)