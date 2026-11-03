package com.example.aquatrackx.data.remote

import com.example.aquatrackx.core.ChlorineRules
import com.example.aquatrackx.data.local.MedicionEntity
import com.example.aquatrackx.data.local.PuntoMedicionEntity
import com.example.aquatrackx.domain.model.OrigenMedicion
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * API REST simulada del sistema de monitoreo de cloro (preguntas 11 y 30).
 *
 * En el MVP académico no existe un backend real: esta clase emula las respuestas
 * de un servicio REST con latencia de red y valores ficticios. Para conectar un
 * backend real basta con implementar [ChlorineApi] con Retrofit manteniendo el
 * mismo contrato, sin tocar la UI ni los repositorios.
 */
interface ChlorineApi {
    /** GET /mediciones/ultimas — devuelve una medición nueva por punto (frecuencia 5 min). */
    suspend fun ultimasMediciones(puntos: List<PuntoMedicionEntity>): List<MedicionEntity>

    /** POST /mediciones — sincroniza una medición registrada sin conexión. */
    suspend fun sincronizarMedicion(medicion: MedicionEntity): Boolean
}

class ChlorineApiSimulada : ChlorineApi {

    private val rnd = Random(System.currentTimeMillis())

    /** Deriva por punto para que las series sean creíbles y con estados variados. */
    private val deriva = mutableMapOf<Long, Double>()

    override suspend fun ultimasMediciones(puntos: List<PuntoMedicionEntity>): List<MedicionEntity> {
        // Latencia de red simulada (300-800 ms).
        delay(rnd.nextLong(300, 800))
        val ahora = System.currentTimeMillis()
        return puntos.map { punto ->
            val base = deriva.getOrPut(punto.id) {
                ChlorineRules.RANGO_NORMAL_MIN + rnd.nextDouble() * 1.4
            }
            var valor = base + rnd.nextDouble() * 0.6 - 0.29
            // Eventos ocasionales para demostrar estados y alertas en vivo.
            when (rnd.nextInt(60)) {
                0 -> valor = rnd.nextDouble(0.6, 1.9)
                1 -> valor = rnd.nextDouble(6.3, 7.4)
                2 -> valor = rnd.nextDouble(2.1, 2.9)
                3 -> valor = rnd.nextDouble(5.2, 5.9)
            }
            valor = valor.coerceIn(0.5, 7.5)
            deriva[punto.id] = valor
            val redondeado = ChlorineRules.redondear(valor)
            MedicionEntity(
                puntoId = punto.id,
                valor = redondeado,
                fecha = ahora,
                estado = ChlorineRules.clasificar(
                    redondeado, punto.normalMin, punto.normalMax,
                    punto.criticoMin, punto.criticoMax
                ),
                origen = OrigenMedicion.API
            )
        }
    }

    override suspend fun sincronizarMedicion(medicion: MedicionEntity): Boolean {
        delay(rnd.nextLong(200, 500))
        // El "servidor" acepta siempre en el MVP.
        return true
    }
}
