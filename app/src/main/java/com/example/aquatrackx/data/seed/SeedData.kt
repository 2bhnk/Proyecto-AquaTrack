package com.example.aquatrackx.data.seed

import com.example.aquatrackx.core.ChlorineRules
import com.example.aquatrackx.core.PasswordHash
import com.example.aquatrackx.data.local.AccionCorrectivaEntity
import com.example.aquatrackx.data.local.AlertaEntity
import com.example.aquatrackx.data.local.AppDatabase
import com.example.aquatrackx.data.local.GranjaEntity
import com.example.aquatrackx.data.local.MedicionEntity
import com.example.aquatrackx.data.local.PuntoMedicionEntity
import com.example.aquatrackx.data.local.UsuarioEntity
import com.example.aquatrackx.domain.model.EstadoAlerta
import com.example.aquatrackx.domain.model.EstadoAccion
import com.example.aquatrackx.domain.model.OrigenMedicion
import com.example.aquatrackx.domain.model.Rol
import com.example.aquatrackx.domain.model.TipoDesviacion
import com.example.aquatrackx.domain.model.TipoPunto
import kotlin.random.Random

/**
 * Datos ficticios iniciales (preguntas 10, 16 y 34 del caso):
 *  - 3 granjas con 4 puntos de medición cada una (12 en total)
 *  - Usuarios demo por rol (operario, supervisor, jefatura)
 *  - Histórico ficticio de 30 días con frecuencia simulada de 5 minutos
 *  - Alertas de ejemplo (abiertas y cerradas) con acciones correctivas
 *
 * Todo es data simulada: no se conecta con sistemas productivos reales (restricción 5.1).
 */
object SeedData {

    const val EMAIL_OPERARIO = "operario@duoc.cl"
    const val EMAIL_SUPERVISOR = "supervisor@duoc.cl"
    const val EMAIL_JEFATURA = "jefatura@duoc.cl"
    const val PASSWORD_DEMO = "123456"

    /** Intervalo simulado de medición: 5 minutos (pregunta 10). */
    const val INTERVALO_MEDICION_MS = 5 * 60 * 1000L

    private val RND = Random(20260831)

    suspend fun poblarSiVacia(db: AppDatabase) {
        if (db.usuarioDao().contar() > 0) return

        // ---- Usuarios (rol principal por usuario, pregunta 15) ----
        db.usuarioDao().insertarTodos(
            listOf(
                UsuarioEntity(
                    email = EMAIL_OPERARIO,
                    nombre = "Ana Operaria",
                    passwordHash = PasswordHash.hash(PASSWORD_DEMO),
                    rol = Rol.OPERARIO,
                    granjaIds = listOf(1L) // solo su granja asignada (pregunta 14)
                ),
                UsuarioEntity(
                    email = EMAIL_SUPERVISOR,
                    nombre = "Benito Supervisor",
                    passwordHash = PasswordHash.hash(PASSWORD_DEMO),
                    rol = Rol.SUPERVISOR,
                    granjaIds = listOf(1L, 2L) // puntos bajo su responsabilidad
                ),
                UsuarioEntity(
                    email = EMAIL_JEFATURA,
                    nombre = "Carla Jefatura",
                    passwordHash = PasswordHash.hash(PASSWORD_DEMO),
                    rol = Rol.JEFATURA,
                    granjaIds = emptyList() // vista general (pregunta 14)
                )
            )
        )

        // ---- Granjas (pregunta 10: 3 granjas) ----
        db.granjaDao().insertarTodas(
            listOf(
                GranjaEntity(1, "Granja Los Aromos", "GR-01", "Región de O'Higgins"),
                GranjaEntity(2, "Granja El Maitén", "GR-02", "Región de Maule"),
                GranjaEntity(3, "Granja Las Palmas", "GR-03", "Región del Libertador")
            )
        )

        // ---- Puntos de medición: 4 por granja (preguntas 7 y 10) ----
        val puntos = mutableListOf<PuntoMedicionEntity>()
        val tipos = listOf(
            TipoPunto.TANQUE to "Tanque de reserva",
            TipoPunto.LINEA to "Línea principal",
            TipoPunto.BEDEDERO to "Bebedero norte",
            TipoPunto.BEDEDERO to "Bebedero sur"
        )
        for (granjaId in 1..3L) {
            tipos.forEachIndexed { index, (tipo, nombre) ->
                val nro = index + 1
                puntos += PuntoMedicionEntity(
                    id = (granjaId - 1) * 4 + nro,
                    granjaId = granjaId,
                    codigo = "P${granjaId.toString().padStart(2, '0')}-$nro",
                    nombre = nombre,
                    tipo = tipo,
                    galpon = "Galpón $nro",
                    ubicacion = when (tipo) {
                        TipoPunto.TANQUE -> "Sector norte, junto a caseta"
                        TipoPunto.LINEA -> "Sala de bombeo"
                        TipoPunto.BEDEDERO -> "Interior ${galponLabel(nro)}"
                    },
                    normalMin = ChlorineRules.RANGO_NORMAL_MIN,
                    normalMax = ChlorineRules.RANGO_NORMAL_MAX,
                    criticoMin = ChlorineRules.RANGO_CRITICO_MIN,
                    criticoMax = ChlorineRules.RANGO_CRITICO_MAX
                )
            }
        }
        db.puntoMedicionDao().insertarTodos(puntos)

        // ---- Histórico ficticio: 30 días con muestreo cada 30 minutos por punto ----
        // (la API simulada continúa generando datos cada 5 minutos en uso, pregunta 10)
        val ahora = System.currentTimeMillis()
        val desde = ahora - 30L * 24 * 60 * 60 * 1000
        val pasoHistorico = 30 * 60 * 1000L
        val mediciones = mutableListOf<MedicionEntity>()
        for (punto in puntos) {
            // Base estable por punto dentro del rango normal, con deriva lenta.
            var valor = ChlorineRules.RANGO_NORMAL_MIN + RND.nextDouble() * 1.5
            var t = desde
            while (t <= ahora) {
                valor += RND.nextDouble() * 0.5 - 0.24
                valor = valor.coerceIn(0.8, 7.2)
                // Eventos ocasionales de desviación para poblar alertas.
                when (RND.nextInt(200)) {
                    0 -> valor = RND.nextDouble(0.5, 1.9)      // crítica por déficit
                    1 -> valor = RND.nextDouble(6.3, 7.5)      // crítica por exceso
                    2 -> valor = RND.nextDouble(2.1, 2.9)      // advertencia por déficit
                    3 -> valor = RND.nextDouble(5.2, 5.9)      // advertencia por exceso
                }
                val redondeado = ChlorineRules.redondear(valor)
                mediciones += MedicionEntity(
                    puntoId = punto.id,
                    valor = redondeado,
                    fecha = t,
                    estado = ChlorineRules.clasificar(
                        redondeado, punto.normalMin, punto.normalMax,
                        punto.criticoMin, punto.criticoMax
                    ),
                    origen = OrigenMedicion.API
                )
                t += pasoHistorico
            }
        }
        db.medicionDao().insertarTodas(mediciones)

        // ---- Alertas de ejemplo (preguntas 19-24) ----
        val criticaAbierta = mediciones
            .filter { it.estado.name == "CRITICO" }
            .maxByOrNull { it.fecha }
        val advertenciaAbierta = mediciones
            .filter { it.estado.name == "ADVERTENCIA" }
            .minByOrNull { kotlin.math.abs(it.fecha - (ahora - 6 * 60 * 60 * 1000)) }
        val cerrada = mediciones
            .filter { it.estado.name == "ADVERTENCIA" }
            .minByOrNull { kotlin.math.abs(it.fecha - (ahora - 3 * 24 * 60 * 60 * 1000)) }

        val alertas = mutableListOf<AlertaEntity>()
        criticaAbierta?.let {
            alertas += AlertaEntity(
                puntoId = it.puntoId,
                medicionId = it.id,
                tipo = if (it.valor < 3) TipoDesviacion.INSUFICIENTE else TipoDesviacion.EXCESIVO,
                severidad = it.estado,
                estado = EstadoAlerta.ABIERTA,
                mensaje = mensajePara(it.valor, it.puntoId, puntos),
                valor = it.valor,
                creadaEn = it.fecha
            )
        }
        advertenciaAbierta?.let {
            alertas += AlertaEntity(
                puntoId = it.puntoId,
                medicionId = it.id,
                tipo = if (it.valor < 3) TipoDesviacion.INSUFICIENTE else TipoDesviacion.EXCESIVO,
                severidad = it.estado,
                estado = EstadoAlerta.ABIERTA,
                mensaje = mensajePara(it.valor, it.puntoId, puntos),
                valor = it.valor,
                creadaEn = it.fecha
            )
        }
        cerrada?.let {
            alertas += AlertaEntity(
                puntoId = it.puntoId,
                medicionId = it.id,
                tipo = if (it.valor < 3) TipoDesviacion.INSUFICIENTE else TipoDesviacion.EXCESIVO,
                severidad = it.estado,
                estado = EstadoAlerta.CERRADA,
                mensaje = mensajePara(it.valor, it.puntoId, puntos),
                valor = it.valor,
                creadaEn = it.fecha,
                cerradaEn = it.fecha + 45 * 60 * 1000,
                cerradaPor = EMAIL_SUPERVISOR
            )
        }
        if (alertas.isNotEmpty()) db.alertaDao().insertarTodas(alertas)

        // ---- Acción correctiva de la alerta cerrada (pregunta 23) ----
        alertas.firstOrNull { it.estado == EstadoAlerta.CERRADA }?.let { alerta ->
            db.accionCorrectivaDao().insertar(
                AccionCorrectivaEntity(
                    alertaId = alerta.id,
                    puntoId = alerta.puntoId,
                    descripcion = "Se ajustó dosificación de cloro y se verificó lectura en rango",
                    responsable = "Benito Supervisor",
                    fecha = alerta.creadaEn,
                    hora = "10:45",
                    estado = EstadoAccion.VERIFICADA,
                    creadaPor = EMAIL_SUPERVISOR
                )
            )
        }
    }

    private fun galponLabel(nro: Int) = when (nro) {
        1 -> "A"
        2 -> "B"
        3 -> "C"
        else -> "D"
    }

    private fun mensajePara(valor: Double, puntoId: Long, puntos: List<PuntoMedicionEntity>): String {
        val punto = puntos.firstOrNull { it.id == puntoId }
        val tipo = if (valor < 3) "bajo" else "alto"
        return "Cloro $tipo en ${punto?.nombre ?: "punto"}: ${ChlorineRules.formatoPpm(valor)} ppm"
    }
}
