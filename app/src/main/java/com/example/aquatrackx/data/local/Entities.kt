package com.example.aquatrackx.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.aquatrackx.domain.model.EstadoAlerta
import com.example.aquatrackx.domain.model.EstadoAccion
import com.example.aquatrackx.domain.model.EstadoCloro
import com.example.aquatrackx.domain.model.OrigenMedicion
import com.example.aquatrackx.domain.model.Rol
import com.example.aquatrackx.domain.model.TipoDesviacion
import com.example.aquatrackx.domain.model.TipoPunto

/**
 * Entidades de persistencia local (Room) — IE 2.3.1.
 * Corresponden a la información definida en las preguntas 7, 13, 23 y 24 del caso.
 */

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val email: String,
    val nombre: String,
    val passwordHash: String,
    val rol: Rol,
    /** IDs de granjas asignadas al usuario (pregunta 14). Vacío = todas (jefatura). */
    val granjaIds: List<Long>
)

@Entity(tableName = "granjas")
data class GranjaEntity(
    @PrimaryKey val id: Long,
    val nombre: String,
    val codigo: String,
    val ubicacion: String
)

@Entity(
    tableName = "puntos_medicion",
    indices = [Index("granjaId")]
)
data class PuntoMedicionEntity(
    @PrimaryKey val id: Long,
    val granjaId: Long,
    val codigo: String,
    val nombre: String,
    val tipo: TipoPunto,
    val galpon: String,
    val ubicacion: String,
    /** Rangos configurables por punto (pregunta 4), en ppm. */
    val normalMin: Double,
    val normalMax: Double,
    val criticoMin: Double,
    val criticoMax: Double
)

@Entity(
    tableName = "mediciones",
    indices = [Index("puntoId"), Index("fecha")]
)
data class MedicionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val puntoId: Long,
    val valor: Double,
    val fecha: Long,
    val estado: EstadoCloro,
    val origen: OrigenMedicion,
    val registradoPor: String? = null,
    /** true = registrada sin conexión y pendiente de sincronizar (pregunta 12). */
    val pendienteSincronizacion: Boolean = false
)

@Entity(
    tableName = "alertas",
    indices = [Index("puntoId"), Index("estado")]
)
data class AlertaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val puntoId: Long,
    val medicionId: Long,
    val tipo: TipoDesviacion,
    val severidad: EstadoCloro,
    val estado: EstadoAlerta,
    val mensaje: String,
    val valor: Double,
    val creadaEn: Long,
    val cerradaEn: Long? = null,
    val cerradaPor: String? = null
)

@Entity(
    tableName = "acciones_correctivas",
    indices = [Index("alertaId")]
)
data class AccionCorrectivaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alertaId: Long,
    val puntoId: Long,
    val descripcion: String,
    val responsable: String,
    val fecha: Long,
    val hora: String,
    val estado: EstadoAccion,
    val creadaPor: String
)
