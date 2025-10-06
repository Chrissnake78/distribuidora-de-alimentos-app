package com.christian.distribuidoradealimentosapp.domain

object DispatchRules {
    fun costoDespacho(totalCompra: Int, km: Double): Int = when {
        totalCompra >= 50000 && km <= 20.0 -> 0
        totalCompra in 25000..49999       -> (150.0 * km).toInt()
        else                              -> (300.0 * km).toInt()
    }
}