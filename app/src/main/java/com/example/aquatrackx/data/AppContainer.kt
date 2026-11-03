package com.example.aquatrackx.data

import android.content.Context
import com.example.aquatrackx.core.AppPreferences
import com.example.aquatrackx.data.local.AppDatabase
import com.example.aquatrackx.data.remote.ChlorineApi
import com.example.aquatrackx.data.remote.ChlorineApiSimulada
import com.example.aquatrackx.data.repository.AlertaRepository
import com.example.aquatrackx.data.repository.AuthRepository
import com.example.aquatrackx.data.repository.CatalogoRepository
import com.example.aquatrackx.data.repository.MedicionRepository
import com.example.aquatrackx.data.seed.SeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Contenedor de dependencias (inyección manual, sin librerías externas).
 * Centraliza la creación de BD, API y repositorios para que las pantallas
 * reciban sus dependencias de forma desacoplada (arquitectura modular, IE 2.3.1).
 */
class AppContainer(context: Context) {

    val prefs = AppPreferences(context)
    val database = AppDatabase.obtener(context)
    val api: ChlorineApi = ChlorineApiSimulada()

    val authRepository = AuthRepository(database, prefs)
    val catalogoRepository = CatalogoRepository(database)
    val medicionRepository = MedicionRepository(database, api, prefs)
    val alertaRepository = AlertaRepository(database)

    private val alcanceSeed = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Puebla los datos ficticios en segundo plano si la BD está vacía. */
    fun inicializarDatos() {
        alcanceSeed.launch {
            SeedData.poblarSiVacia(database)
        }
    }
}
