package com.example.aquatrackx.core.validacion

/**
 * Resultado genérico de una validación: un mapa campo -> mensaje de error.
 * Mapa vacío significa que el formulario es válido.
 *
 * Esta estructura permite que la lógica de validación viva fuera de la interfaz (IE 2.2.1)
 * y que cualquier pantalla solo se encargue de mostrar los mensajes.
 */
data class ResultadoValidacion(
    val errores: Map<String, String> = emptyMap()
) {
    val esValido: Boolean get() = errores.isEmpty()

    operator fun get(campo: String): String? = errores[campo]

    companion object {
        val Vacio = ResultadoValidacion()
    }
}

/** Claves de campo usadas por los formularios de la aplicación. */
object Campos {
    const val EMAIL = "email"
    const val PASSWORD = "password"
    const val PUNTO = "punto"
    const val VALOR = "valor"
    const val DESCIPCION = "descripcion"
    const val RESPONSABLE = "responsable"
}
