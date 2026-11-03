package com.example.aquatrackx.ui.navegacion

/**
 * Rutas de navegación entre vistas (navegación coherente, IE 2.1.1).
 */
object Rutas {
    const val LOGIN = "login"
    const val PANEL = "panel"
    const val PUNTOS = "puntos"
    const val ALERTAS = "alertas"
    const val HISTORIAL = "historial"
    const val PERFIL = "perfil"

    const val DETALLE_PUNTO = "punto/{puntoId}"
    fun detallePunto(puntoId: Long) = "punto/$puntoId"

    const val REGISTRO = "registro"
}
