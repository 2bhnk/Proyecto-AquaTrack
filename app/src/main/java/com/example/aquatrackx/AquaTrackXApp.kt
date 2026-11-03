package com.example.aquatrackx

import android.app.Application
import com.example.aquatrackx.data.AppContainer
import com.example.aquatrackx.service.NotificationHelper

/**
 * Application: crea el contenedor de dependencias, el canal de notificaciones
 * nativo y dispara la carga de los datos ficticios iniciales.
 */
class AquaTrackXApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.crearCanal(this)
        container.inicializarDatos()
    }
}
