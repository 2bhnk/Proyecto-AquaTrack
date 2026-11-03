package com.example.aquatrackx.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.aquatrackx.domain.model.EstadoAlerta
import com.example.aquatrackx.domain.model.EstadoCloro
import kotlinx.coroutines.flow.Flow

/**
 * DAOs de la persistencia local (Room). Exponen [Flow] para que la UI reaccione
 * automáticamente a cambios de estado (IE 2.2.1: componentes visuales que responden
 * a cambios de estado).
 */

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun porEmail(email: String): UsuarioEntity?

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(usuarios: List<UsuarioEntity>)
}

@Dao
interface GranjaDao {
    @Query("SELECT * FROM granjas ORDER BY nombre")
    fun todas(): Flow<List<GranjaEntity>>

    @Query("SELECT * FROM granjas ORDER BY nombre")
    suspend fun todasSuspend(): List<GranjaEntity>

    @Query("SELECT * FROM granjas WHERE id IN (:ids) ORDER BY nombre")
    fun porIds(ids: List<Long>): Flow<List<GranjaEntity>>

    @Query("SELECT COUNT(*) FROM granjas")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(granjas: List<GranjaEntity>)
}

@Dao
interface PuntoMedicionDao {
    @Query("SELECT * FROM puntos_medicion ORDER BY granjaId, codigo")
    fun todos(): Flow<List<PuntoMedicionEntity>>

    @Query("SELECT * FROM puntos_medicion WHERE granjaId IN (:granjaIds) ORDER BY granjaId, codigo")
    fun porGranjas(granjaIds: List<Long>): Flow<List<PuntoMedicionEntity>>

    @Query("SELECT * FROM puntos_medicion ORDER BY granjaId, codigo")
    suspend fun todosSuspend(): List<PuntoMedicionEntity>

    @Query("SELECT * FROM puntos_medicion WHERE id = :id")
    suspend fun porId(id: Long): PuntoMedicionEntity?

    @Query("SELECT * FROM puntos_medicion WHERE id = :id")
    fun porIdFlow(id: Long): Flow<PuntoMedicionEntity?>

    @Query("SELECT COUNT(*) FROM puntos_medicion")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(puntos: List<PuntoMedicionEntity>)
}

@Dao
interface MedicionDao {

    @Query("SELECT * FROM mediciones WHERE puntoId = :puntoId ORDER BY fecha DESC")
    fun porPunto(puntoId: Long): Flow<List<MedicionEntity>>

    @Query("SELECT * FROM mediciones WHERE puntoId = :puntoId ORDER BY fecha DESC LIMIT 1")
    fun ultimaDePunto(puntoId: Long): Flow<MedicionEntity?>

    @Query("SELECT * FROM mediciones WHERE puntoId = :puntoId ORDER BY fecha DESC LIMIT 1")
    suspend fun ultimaMedicionSuspend(puntoId: Long): MedicionEntity?

    /**
     * Última medición de cada punto (para el panel principal).
     * SQLite: para cada punto toma la fila con fecha máxima.
     */
    @Query(
        """
        SELECT m.* FROM mediciones m
        INNER JOIN (
            SELECT puntoId, MAX(fecha) AS maxFecha
            FROM mediciones
            GROUP BY puntoId
        ) ult ON m.puntoId = ult.puntoId AND m.fecha = ult.maxFecha
        """
    )
    fun ultimasDeTodosLosPuntos(): Flow<List<MedicionEntity>>

    @Query("SELECT * FROM mediciones WHERE fecha >= :desde ORDER BY fecha DESC")
    fun desde(desde: Long): Flow<List<MedicionEntity>>

    @Query(
        """
        SELECT * FROM mediciones
        WHERE fecha >= :desde AND puntoId IN (:puntoIds)
        ORDER BY fecha DESC
        """
    )
    fun desdeEnPuntos(desde: Long, puntoIds: List<Long>): Flow<List<MedicionEntity>>

    @Query(
        """
        SELECT * FROM mediciones
        WHERE fecha >= :desde AND puntoId IN (:puntoIds)
        ORDER BY fecha ASC
        """
    )
    suspend fun listarDesdeEnPuntos(desde: Long, puntoIds: List<Long>): List<MedicionEntity>

    @Query("SELECT * FROM mediciones WHERE pendienteSincronizacion = 1")
    suspend fun pendientesDeSincronizacion(): List<MedicionEntity>

    @Query("SELECT COUNT(*) FROM mediciones WHERE pendienteSincronizacion = 1")
    fun contarPendientes(): Flow<Int>

    @Query("UPDATE mediciones SET pendienteSincronizacion = 0 WHERE id = :id")
    suspend fun marcarSincronizada(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(medicion: MedicionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(mediciones: List<MedicionEntity>)

    @Query("SELECT COUNT(*) FROM mediciones")
    suspend fun contar(): Int

    // ---- Agregados para el panel (pregunta 28: promedios, máximos y mínimos) ----

    @Query("SELECT AVG(valor) FROM mediciones WHERE fecha >= :desde AND puntoId IN (:puntoIds)")
    suspend fun promedioDesde(desde: Long, puntoIds: List<Long>): Double?

    @Query("SELECT MAX(valor) FROM mediciones WHERE fecha >= :desde AND puntoId IN (:puntoIds)")
    suspend fun maximoDesde(desde: Long, puntoIds: List<Long>): Double?

    @Query("SELECT MIN(valor) FROM mediciones WHERE fecha >= :desde AND puntoId IN (:puntoIds)")
    suspend fun minimoDesde(desde: Long, puntoIds: List<Long>): Double?
}

@Dao
interface AlertaDao {
    @Query("SELECT * FROM alertas ORDER BY creadaEn DESC")
    fun todas(): Flow<List<AlertaEntity>>

    @Query("SELECT * FROM alertas WHERE estado = :estado ORDER BY creadaEn DESC")
    fun porEstado(estado: EstadoAlerta): Flow<List<AlertaEntity>>

    @Query("SELECT * FROM alertas WHERE id = :id")
    fun porId(id: Long): Flow<AlertaEntity?>

    @Query("SELECT * FROM alertas WHERE id = :id")
    suspend fun porIdSuspend(id: Long): AlertaEntity?

    @Query("SELECT * FROM alertas WHERE puntoId = :puntoId AND estado = 'ABIERTA' ORDER BY creadaEn DESC LIMIT 1")
    suspend fun abiertaDePunto(puntoId: Long): AlertaEntity?

    @Query("SELECT * FROM alertas WHERE puntoId IN (:puntoIds) AND estado = :estado ORDER BY creadaEn DESC")
    fun porEstadoEnPuntos(puntoIds: List<Long>, estado: EstadoAlerta): Flow<List<AlertaEntity>>

    @Query("SELECT COUNT(*) FROM alertas WHERE estado = 'ABIERTA'")
    fun contarAbiertas(): Flow<Int>

    @Query("SELECT COUNT(*) FROM alertas WHERE estado = 'ABIERTA' AND severidad = 'CRITICO'")
    fun contarCriticasAbiertas(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(alerta: AlertaEntity): Long

    @Update
    suspend fun actualizar(alerta: AlertaEntity)

    @Query("SELECT COUNT(*) FROM alertas")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(alertas: List<AlertaEntity>)
}

@Dao
interface AccionCorrectivaDao {
    @Query("SELECT * FROM acciones_correctivas ORDER BY fecha DESC")
    fun todas(): Flow<List<AccionCorrectivaEntity>>

    @Query("SELECT * FROM acciones_correctivas WHERE alertaId = :alertaId ORDER BY fecha DESC")
    fun porAlerta(alertaId: Long): Flow<List<AccionCorrectivaEntity>>

    @Query("SELECT COUNT(*) FROM acciones_correctivas WHERE alertaId = :alertaId")
    suspend fun contarDeAlerta(alertaId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(accion: AccionCorrectivaEntity): Long

    @Update
    suspend fun actualizar(accion: AccionCorrectivaEntity)

    @Query("SELECT COUNT(*) FROM acciones_correctivas")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(acciones: List<AccionCorrectivaEntity>)
}
