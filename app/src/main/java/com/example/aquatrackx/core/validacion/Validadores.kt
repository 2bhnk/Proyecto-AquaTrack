package com.example.aquatrackx.core.validacion

/**
 * Validadores centralizados de los formularios (IE 2.2.1).
 * Son funciones puras: no conocen Compose ni Android, por lo que pueden
 * reutilizarse desde cualquier ViewModel y probarse con JUnit.
 */
object LoginValidator {

    // Formato de correo académico/ficticio (pregunta 16: correo y contraseña, sin RUT).
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    const val PASSWORD_MIN = 6

    fun validar(email: String, password: String): ResultadoValidacion {
        val errores = mutableMapOf<String, String>()

        when {
            email.isBlank() -> errores[Campos.EMAIL] = "Ingresa tu correo"
            !EMAIL_REGEX.matches(email.trim()) ->
                errores[Campos.EMAIL] = "Formato de correo inválido (ej: operario@duoc.cl)"
        }

        when {
            password.isBlank() -> errores[Campos.PASSWORD] = "Ingresa tu contraseña"
            password.length < PASSWORD_MIN ->
                errores[Campos.PASSWORD] = "Debe tener al menos $PASSWORD_MIN caracteres"
        }

        return ResultadoValidacion(errores)
    }
}

object MedicionValidator {

    /** Límites instrumentales del MVP: 0,00 a 20,00 ppm con máximo 2 decimales (pregunta 3). */
    const val VALOR_MIN = 0.0
    const val VALOR_MAX = 20.0
    const val MAX_DECIMALES = 2

    /**
     * @param puntoId punto de medición seleccionado (null si no se eligió).
     * @param valorLibre texto tal cual lo escribe el usuario (acepta coma o punto decimal).
     */
    fun validar(puntoId: Long?, valorLibre: String): ResultadoValidacion {
        val errores = mutableMapOf<String, String>()

        if (puntoId == null) {
            errores[Campos.PUNTO] = "Selecciona un punto de medición"
        }

        val texto = valorLibre.trim()
        when {
            texto.isBlank() -> errores[Campos.VALOR] = "Ingresa el valor de cloro libre"
            else -> {
                val normalizado = texto.replace(',', '.')
                val numero = normalizado.toDoubleOrNull()
                when {
                    numero == null -> errores[Campos.VALOR] = "Debe ser un número (ej: 3,50)"
                    numero < VALOR_MIN || numero > VALOR_MAX ->
                        errores[Campos.VALOR] =
                            "Debe estar entre ${formato(VALOR_MIN)} y ${formato(VALOR_MAX)} ppm"
                    normalizado.contains('.') &&
                        normalizado.substringAfter('.').length > MAX_DECIMALES ->
                        errores[Campos.VALOR] = "Usa máximo $MAX_DECIMALES decimales"
                }
            }
        }

        return ResultadoValidacion(errores)
    }

    /** Convierte el texto validado a número (coma o punto decimal). */
    fun aNumero(valorLibre: String): Double =
        valorLibre.trim().replace(',', '.').toDouble()

    private fun formato(v: Double) =
        String.format(java.util.Locale.US, "%.2f", v).replace('.', ',')
}

object AccionCorrectivaValidator {

    const val DESCRIPCION_MIN = 10
    const val RESPONSABLE_MIN = 3

    fun validar(descripcion: String, responsable: String): ResultadoValidacion {
        val errores = mutableMapOf<String, String>()

        when {
            descripcion.isBlank() ->
                errores[Campos.DESCIPCION] = "Describe la acción correctiva"
            descripcion.trim().length < DESCRIPCION_MIN ->
                errores[Campos.DESCIPCION] =
                    "Detalla al menos $DESCRIPCION_MIN caracteres"
        }

        when {
            responsable.isBlank() ->
                errores[Campos.RESPONSABLE] = "Indica el responsable"
            responsable.trim().length < RESPONSABLE_MIN ->
                errores[Campos.RESPONSABLE] = "Nombre demasiado corto"
        }

        return ResultadoValidacion(errores)
    }
}
