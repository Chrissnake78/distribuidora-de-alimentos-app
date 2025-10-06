package com.christian.distribuidoradealimentosapp.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// NO dupliques esta línea en otro archivo:
val Context.dataStore by preferencesDataStore(name = "settings")

private object SettingsKeys {
    val MIN_TEMP = doublePreferencesKey("min_temp")
    val MAX_TEMP = doublePreferencesKey("max_temp")
}

class SettingsDataStore(private val context: Context) {

    // Valores por defecto exigidos por la pauta: 2.0 °C a 8.0 °C
    val minTemp: Flow<Double> = context.dataStore.data.map { it[SettingsKeys.MIN_TEMP] ?: 2.0 }
    val maxTemp: Flow<Double> = context.dataStore.data.map { it[SettingsKeys.MAX_TEMP] ?: 8.0 }

    suspend fun saveMinTemp(value: Double) {
        context.dataStore.edit { it[SettingsKeys.MIN_TEMP] = value }
    }

    suspend fun saveMaxTemp(value: Double) {
        context.dataStore.edit { it[SettingsKeys.MAX_TEMP] = value }
    }
}