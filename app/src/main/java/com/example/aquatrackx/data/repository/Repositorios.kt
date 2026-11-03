package com.example.aquatrackx.data.repository

import com.example.aquatrackx.core.AppPreferences
import com.example.aquatrackx.core.ChlorineRules
import com.example.aquatrackx.core.PasswordHash
import com.example.aquatrackx.data.local.AccionCorrectivaEntity
import com.example.aquatrackx.data.local.AlertaEntity
import com.example.aquatrackx.data.local.AppDatabase
import com.example.aquatrackx.data.local.GranjaEntity
import com.example.aquatrackx.data.local.MedicionEntity
import com.example.aquatrackx.data.local.PuntoMedicionEntity
import com.example.aquatrackx.data.local.UsuarioEntity
import com.example.aquatrackx.data.remote.ChlorineApi
import com.example.aquatrackx.domain.model.EstadoAlerta
import com.example.aquatrackx.domain.model.EstadoAccion
import com.example.aquatrackx.domain.model.EstadoCloro
import com.example.aquatrackx.domain.model.OrigenMedicion
import com.example.aquatrackx.domain.model.Rol
import com.example.aquatrackx.domain.model.TipoDesviacion
import com.example.aquatrackx.domain.model.TipoPunto
import kotlinx.coroutines.flow.Flow

/**
 * Resultado de autenticación para la UI.
 */
sealed interface ResultadoLogin {
    data class Exitoso(val usuario: UsuarioEntity) : ResultadoLogin
    data class Error(val mensaje: String) : ResultadoLogin
}

/**
 * Repositorio de autenticación: consulta la base local y gestiona la sesión.
 * La validación del formulario vive en [com.example.aquatrackx.core.validacion];
 * aquí solo se resuelve el acceso (pregunta 16: correo y contraseña ficticios).
 */
class AuthRepository(
    private val db: AppDatabase,
    private val prefs: AppPreferences
) {
    suspend fun login(email: String, password: String): ResultadoLogin {
        val usuario = db.usuarioDao().porEmail(email.trim().lowercase())
            ?: return ResultadoLogin.Error("Usuario o contraseña incorrectos")
        if (!PasswordHash.verificar(password, usuario.passwordHash)) {
            return ResultadoLogin.Error("Usuario o contraseña incorrectos")
        }
        prefs.emailSesion = usuario.email
        return ResultadoLogin.Exitoso(usuario)
    }

    suspend fun usuarioSesion(): UsuarioEntity? =
        prefs.emailSesion?.let { db.usuarioDao().porEmail(it) }

    fun cerrarSesion() = prefs.limpiarSesion()

    val modoSinConexion: Boolean
        get() = prefs.modoSinConexion

    fun setModoSinConexion(valor: Boolean) {
        prefs.modoSinConexion = valor
    }
}

/**
 * Repositorio de catálogo (granjas y puntos) con filtrado por rol (pregunta 14):
 * el operario ve sus granjas asignadas, el supervisor sus puntos y la jefatura todo.
 */
class CatalogoRepository(private val db: AppDatabase) {

    fun granjasVisibles(usuario: UsuarioEntity): Flow<List<GranjaEntity>> =
        if (usuario.granjaIds.isEmpty()) db.granjaDao().todas()
        else db.granjaDao().porIds(usuario.granjaIds)

    fun puntosVisibles(usuario: UsuarioEntity): Flow<List<PuntoMedicionEntity>> =
        if (usuario.granjaIds.isEmpty()) db.puntoMedicionDao().todos()
        else db.puntoMedicionDao().porGranjas(usuario.granjaIds)

    suspend fun puntosSuspend(): List<PuntoMedicionEntity> = db.puntoMedicionDao().todosSuspend()

    suspend fun punto(id: Long): PuntoMedicionEntity? = db.puntoMedicionDao().porId(id)

    fun puntoFlow(id: Long): Flow<PuntoMedicionEntity?> = db.puntoMedicionDao().porIdFlow(id)

    suspend fun granjasSuspend(): List<GranjaEntity> = db.granjaDao().todasSuspend()
}

/**
 * Repositorio de mediciones: consulta local, alta manual y sincronización con la
 * API simulada (offline-first: Room es la fuente de verdad, la API la enriquece).
 */
class MedicionRepository(
    private val db: AppDatabase,
    private val api: ChlorineApi,
    private val prefs: AppPreferences
) {
    fun medicionesDePunto(puntoId: Long): Flow<List<MedicionEntity>> =
        db.medicionDao().porPunto(puntoId)

    fun ultimaDePunto(puntoId: Long): Flow<MedicionEntity?> =
        db.medicionDao().ultimaDePunto(puntoId)

    fun ultimasDeTodosLosPuntos(): Flow<List<MedicionEntity>> =
        db.medicionDao().ultimasDeTodosLosPuntos()

    fun medicionesDesde(desde: Long, puntoIds: List<Long>): Flow<List<MedicionEntity>> =
        db.medicionDao().desdeEnPuntos(desde, puntoIds)

    fun pendientesDeSincronizacion(): Flow<Int> = db.medicionDao().contarPendientes()

    // ---- Agregados para el panel (pregunta 28) ----

    suspend fun promedioDesde(desde: Long, puntoIds: List<Long>): Double? =
        db.medicionDao().promedioDesde(desde, puntoIds)

    suspend fun maximoDesde(desde: Long, puntoIds: List<Long>): Double? =
        db.medicionDao().maximoDesde(desde, puntoIds)

    suspend fun minimoDesde(desde: Long, puntoIds: List<Long>): Double? =
        db.medicionDao().minimoDesde(desde, puntoIds)

    suspend fun listarDesde(desde: Long, puntoIds: List<Long>): List<MedicionEntity> =
        db.medicionDao().listarDesdeEnPuntos(desde, puntoIds)

    /**
     * Registra una medición manual (pregunta 11). Si el modo sin conexión está
     * activo, queda marcada como pendiente de sincronización (pregunta 12).
     */
    suspend fun registrarManual(
        punto: PuntoMedicionEntity,
        valor: Double,
        usuario: UsuarioEntity
    ): MedicionEntity {
        val redondeado = ChlorineRules.redondear(valor)
        val medicion = MedicionEntity(
            puntoId = punto.id,
            valor = redondeado,
            fecha = System.currentTimeMillis(),
            estado = ChlorineRules.clasificar(
                redondeado, punto.normalMin, punto.normalMax,
                punto.criticoMin, punto.criticoMax
            ),
            origen = OrigenMedicion.MANUAL,
            registradoPor = usuario.email,
            pendienteSincronizacion = prefs.modoSinConexion
        )
        val id = db.medicionDao().insertar(medicion)
        // Toda medición fuera de rango genera su alerta automáticamente (pregunta 19).
        AlertasHelper.evaluarYCrearAlerta(db, medicion.copy(id = id), punto)
        return medicion.copy(id = id)
    }

    /**
     * Sincroniza: recibe nuevas mediciones de la API simulada, las persiste,
     * genera alertas automáticas y envía las mediciones pendientes (pregunta 12).
     * Devuelve las alertas críticas nuevas para disparar notificaciones nativas.
     */
    suspend fun sincronizar(): List<AlertaEntity> {
        if (prefs.modoSinConexion) return emptyList()

        val puntos = db.puntoMedicionDao().todosSuspend()
        if (puntos.isEmpty()) return emptyList()

        val nuevas = api.ultimasMediciones(puntos)
        val criticasNuevas = mutableListOf<AlertaEntity>()

        for (medicion in nuevas) {
            val punto = puntos.firstOrNull { it.id == medicion.puntoId } ?: continue
            db.medicionDao().insertar(medicion)
            AlertasHelper.evaluarYCrearAlerta(db, medicion, punto)?.let { alerta ->
                if (alerta.severidad == EstadoCloro.CRITICO) criticasNuevas += alerta
            }
        }

        // Envío de mediciones registradas sin conexión.
        for (pendiente in db.medicionDao().pendientesDeSincronizacion()) {
            if (api.sincronizarMedicion(pendiente)) {
                db.medicionDao().marcarSincronizada(pendiente.id)
            }
        }

        return criticasNuevas
    }
}

/**
 * Lógica de alertas centralizada: una medición fuera de rango crea su alerta con
 * severidad y tipo de desviación (preguntas 5 y 19). Si ya existe una alerta
 * abierta para el mismo punto, se actualiza el valor en lugar de duplicar.
 */
object AlertasHelper {

    suspend fun evaluarYCrearAlerta(
        db: AppDatabase,
        medicion: MedicionEntity,
        punto: PuntoMedicionEntity
    ): AlertaEntity? {
        if (medicion.estado == EstadoCloro.NORMAL) return null

        val tipo = ChlorineRules.tipoDesviacion(
            medicion.valor, punto.normalMin, punto.normalMax
        ) ?: return null

        val existente = db.alertaDao().abiertaDePunto(punto.id)
        if (existente != null) {
            val actualizada = existente.copy(
                valor = medicion.valor,
                severidad = medicion.estado,
                medicionId = medicion.id,
                mensaje = mensaje(tipo, medicion.valor, punto)
            )
            db.alertaDao().actualizar(actualizada)
            return actualizada
        }

        val alerta = AlertaEntity(
            puntoId = punto.id,
            medicionId = medicion.id,
            tipo = tipo,
            severidad = medicion.estado,
            estado = EstadoAlerta.ABIERTA,
            mensaje = mensaje(tipo, medicion.valor, punto),
            valor = medicion.valor,
            creadaEn = medicion.fecha
        )
        val id = db.alertaDao().insertar(alerta)
        return alerta.copy(id = id)
    }

    private fun mensaje(tipo: TipoDesviacion, valor: Double, punto: PuntoMedicionEntity): String {
        val detalle = when (tipo) {
            TipoDesviacion.INSUFICIENTE -> "cloro insuficiente"
            TipoDesviacion.EXCESIVO -> "cloro excesivo"
        }
        return "${punto.nombre} (${punto.codigo}): $detalle — ${ChlorineRules.formatoPpm(valor)} ppm"
    }
}

/**
 * Repositorio de alertas y acciones correctivas (preguntas 21-24).
 * El cierre de una alerta exige acción correctiva registrada y una medición
 * posterior dentro del rango aceptable, validado por un supervisor.
 */
class AlertaRepository(private val db: AppDatabase) {

    fun todas(): Flow<List<AlertaEntity>> = db.alertaDao().todas()

    fun porEstado(estado: EstadoAlerta): Flow<List<AlertaEntity>> =
        db.alertaDao().porEstado(estado)

    fun porEstadoEnPuntos(puntoIds: List<Long>, estado: EstadoAlerta): Flow<List<AlertaEntity>> =
        db.alertaDao().porEstadoEnPuntos(puntoIds, estado)

    fun porId(id: Long): Flow<AlertaEntity?> = db.alertaDao().porId(id)

    fun contarAbiertas(): Flow<Int> = db.alertaDao().contarAbiertas()

    fun contarCriticasAbiertas(): Flow<Int> = db.alertaDao().contarCriticasAbiertas()

    fun accionesDeAlerta(alertaId: Long): Flow<List<AccionCorrectivaEntity>> =
        db.accionCorrectivaDao().porAlerta(alertaId)

    fun todasLasAcciones(): Flow<List<AccionCorrectivaEntity>> =
        db.accionCorrectivaDao().todas()

    /** Registra una acción correctiva (pregunta 23). */
    suspend fun registrarAccion(
        alerta: AlertaEntity,
        descripcion: String,
        responsable: String,
        creadaPor: String
    ): AccionCorrectivaEntity {
        val ahora = System.currentTimeMillis()
        val accion = AccionCorrectivaEntity(
            alertaId = alerta.id,
            puntoId = alerta.puntoId,
            descripcion = descripcion.trim(),
            responsable = responsable.trim(),
            fecha = ahora,
            hora = horaDe(ahora),
            estado = EstadoAccion.COMPLETADA,
            creadaPor = creadaPor
        )
        val id = db.accionCorrectivaDao().insertar(accion)
        return accion.copy(id = id)
    }

    /**
     * Evalúa las condiciones de cierre (pregunta 22):
     *  1) debe existir al menos una acción correctiva registrada,
     *  2) la última medición del punto debe estar dentro del rango aceptable.
     */
    suspend fun condicionesDeCierre(alerta: AlertaEntity): CondicionCierre {
        val acciones = db.accionCorrectivaDao().contarDeAlerta(alerta.id)
        val ultimaReal = db.medicionDao().ultimaMedicionSuspend(alerta.puntoId)
        val punto = db.puntoMedicionDao().porId(alerta.puntoId)
        val enRango = ultimaReal != null && punto != null &&
            ChlorineRules.dentroDeRango(ultimaReal.valor, punto.normalMin, punto.normalMax)

        return CondicionCierre(
            tieneAccionCorrectiva = acciones > 0,
            medicionVerificada = enRango,
            valorVerificacion = ultimaReal?.valor
        )
    }

    /** Cierra la alerta si las condiciones se cumplen y el rol lo permite (preguntas 21-22). */
    suspend fun cerrarAlerta(
        alerta: AlertaEntity,
        usuario: UsuarioEntity
    ): ResultadoCierre {
        if (usuario.rol != Rol.SUPERVISOR && usuario.rol != Rol.JEFATURA) {
            return ResultadoCierre.Error("Solo supervisores pueden cerrar alertas")
        }
        val condicion = condicionesDeCierre(alerta)
        if (!condicion.tieneAccionCorrectiva) {
            return ResultadoCierre.Error("Registra primero una acción correctiva")
        }
        if (!condicion.medicionVerificada) {
            return ResultadoCierre.Error(
                "Se requiere una medición posterior dentro del rango aceptable"
            )
        }
        val ahora = System.currentTimeMillis()
        db.alertaDao().actualizar(
            alerta.copy(
                estado = EstadoAlerta.CERRADA,
                cerradaEn = ahora,
                cerradaPor = usuario.email
            )
        )
        return ResultadoCierre.Exitoso
    }

    private fun horaDe(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }
}

data class CondicionCierre(
    val tieneAccionCorrectiva: Boolean,
    val medicionVerificada: Boolean,
    val valorVerificacion: Double?
)

sealed interface ResultadoCierre {
    data object Exitoso : ResultadoCierre
    data class Error(val mensaje: String) : ResultadoCierre
}
