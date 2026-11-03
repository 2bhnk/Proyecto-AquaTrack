package com.example.aquatrackx.core

import com.example.aquatrackx.domain.model.EstadoCloro
import com.example.aquatrackx.domain.model.TipoDesviacion
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Reglas de negocio del cloro libre, centralizadas y desacopladas de la interfaz (IE 2.2.1).
 *
 * Rangos del MVP (pregunta 1 del cuestionario):
 *  - Normal:      entre 3 y 5 ppm (inclusive)
 *  - Advertencia: de 2 a menos de 3 ppm, o sobre 5 hasta 6 ppm
 *  - Crítico:     inferior a 2 ppm o superior a 6 ppm
 *
 * Los puntos pueden tener rangos configurables (pregunta 4), por lo que todas las
 * funciones reciben los límites explícitamente.
 */
object ChlorineRules {

    const val RANGO_NORMAL_MIN = 3.0
    const val RANGO_NORMAL_MAX = 5.0
    const val RANGO_CRITICO_MIN = 2.0
    const val RANGO_CRITICO_MAX = 6.0

    /** Valor máximo del eje del medidor (gauge). */
    const val GAUGE_MAX = 8.0

    /** Clasifica un valor dentro de los límites indicados. */
    fun clasificar(
        valor: Double,
        normalMin: Double = RANGO_NORMAL_MIN,
        normalMax: Double = RANGO_NORMAL_MAX,
        criticoMin: Double = RANGO_CRITICO_MIN,
        criticoMax: Double = RANGO_CRITICO_MAX
    ): EstadoCloro = when {
        valor < criticoMin || valor > criticoMax -> EstadoCloro.CRITICO
        valor < normalMin || valor > normalMax -> EstadoCloro.ADVERTENCIA
        else -> EstadoCloro.NORMAL
    }

    /**
     * Determina el tipo de desviación (pregunta 5): valores por debajo del rango normal
     * son "insuficiente" y los del sobre el rango "excesivo". Fuera de rango siempre
     * devuelve un tipo; dentro de rango devuelve null (no hay desviación).
     */
    fun tipoDesviacion(
        valor: Double,
        normalMin: Double = RANGO_NORMAL_MIN,
        normalMax: Double = RANGO_NORMAL_MAX
    ): TipoDesviacion? = when {
        valor < normalMin -> TipoDesviacion.INSUFICIENTE
        valor > normalMax -> TipoDesviacion.EXCESIVO
        else -> null
    }

    /** ¿El valor está dentro del rango aceptable? (condición de cierre de alerta, pregunta 22). */
    fun dentroDeRango(
        valor: Double,
        normalMin: Double = RANGO_NORMAL_MIN,
        normalMax: Double = RANGO_NORMAL_MAX
    ): Boolean = valor in normalMin..normalMax

    /**
     * Formato de presentación con dos decimales en coma decimal (pregunta 3, ej. "3,50 ppm").
     */
    fun formatoPpm(valor: Double): String =
        String.format(Locale.US, "%.2f", valor).replace('.', ',')

    /** Proyecta un valor al rango 0..1 para el medidor animado. */
    fun normalizarParaGauge(valor: Double, max: Double = GAUGE_MAX): Float =
        (valor / max).coerceIn(0.0, 1.0).toFloat()

    /** Redondea a 2 decimales para persistir con la precisión del instrumento (pregunta 3). */
    fun redondear(valor: Double): Double = (valor * 100).roundToInt() / 100.0

    /** Utilidad para detectar cambios relevantes en una serie (usado en gráficos). */
    fun variacion(a: Double, b: Double): Double = abs(a - b)
}
