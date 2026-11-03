package com.example.aquatrackx.core

import android.content.Context

/**
 * Preferencias simples de la sesión y del modo de conectividad.
 * Se mantiene fuera de Room porque son datos ligeros de arranque (sesión persistida
 * para no volver a pedir login en cada apertura) y el interruptor de "modo sin conexión"
 * usado para demostrar el registro de mediciones pendientes de sincronización (pregunta 12).
 */
class AppPreferences(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("aquatrackx_prefs", Context.MODE_PRIVATE)

    var emailSesion: String?
        get() = prefs.getString(KEY_SESION, null)
        set(value) = prefs.edit().putString(KEY_SESION, value).apply()

    var modoSinConexion: Boolean
        get() = prefs.getBoolean(KEY_OFFLINE, false)
        set(value) = prefs.edit().putBoolean(KEY_OFFLINE, value).apply()

    fun limpiarSesion() {
        prefs.edit().remove(KEY_SESION).apply()
    }

    private companion object {
        const val KEY_SESION = "sesion_email"
        const val KEY_OFFLINE = "modo_sin_conexion"
    }
}
