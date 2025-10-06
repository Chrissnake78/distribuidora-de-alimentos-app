package com.christian.distribuidoradealimentosapp.data

data class Order(
    var orderId: String = "",
    var uid: String = "",
    var items: List<CartItem> = emptyList(),
    var totalCompra: Int = 0,
    var distanciaKm: Double = 0.0,
    var costoDespacho: Int = 0,
    var totalPagar: Int = 0,
    var requiereFrio: Boolean = false,
    var estado: String = "CREADO"
)