package com.example.aquatrackx.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.aquatrackx.domain.model.EstadoAlerta
import com.example.aquatrackx.domain.model.EstadoAccion
import com.example.aquatrackx.domain.model.EstadoCloro
import com.example.aquatrackx.domain.model.OrigenMedicion
import com.example.aquatrackx.domain.model.Rol
import com.example.aquatrackx.domain.model.TipoDesviacion
import com.example.aquatrackx.domain.model.TipoPunto

/** Conversores de enums a String para la persistencia local. */
class Converters {
    @TypeConverter fun rolToString(v: Rol): String = v.name
    @TypeConverter fun stringToRol(v: String): Rol = Rol.valueOf(v)

    @TypeConverter fun estadoToString(v: EstadoCloro): String = v.name
    @TypeConverter fun stringToEstado(v: String): EstadoCloro = EstadoCloro.valueOf(v)

    @TypeConverter fun tipoPuntoToString(v: TipoPunto): String = v.name
    @TypeConverter fun stringToTipoPunto(v: String): TipoPunto = TipoPunto.valueOf(v)

    @TypeConverter fun desviacionToString(v: TipoDesviacion): String = v.name
    @TypeConverter fun stringToDesviacion(v: String): TipoDesviacion = TipoDesviacion.valueOf(v)

    @TypeConverter fun estadoAlertaToString(v: EstadoAlerta): String = v.name
    @TypeConverter fun stringToEstadoAlerta(v: String): EstadoAlerta = EstadoAlerta.valueOf(v)

    @TypeConverter fun estadoAccionToString(v: EstadoAccion): String = v.name
    @TypeConverter fun stringToEstadoAccion(v: String): EstadoAccion = EstadoAccion.valueOf(v)

    @TypeConverter fun origenToString(v: OrigenMedicion): String = v.name
    @TypeConverter fun stringToOrigen(v: String): OrigenMedicion = OrigenMedicion.valueOf(v)

    @TypeConverter fun longsToString(v: List<Long>): String = v.joinToString(",")
    @TypeConverter fun stringToLongs(v: String): List<Long> =
        if (v.isBlank()) emptyList() else v.split(",").mapNotNull { it.toLongOrNull() }
}

/**
 * Base de datos local (persistencia — IE 2.3.1).
 * Consulta y registro funcionan sin conexión; la sincronización con la API
 * simulada se encarga de marcar los registros pendientes (pregunta 12).
 */
@Database(
    entities = [
        UsuarioEntity::class,
        GranjaEntity::class,
        PuntoMedicionEntity::class,
        MedicionEntity::class,
        AlertaEntity::class,
        AccionCorrectivaEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun granjaDao(): GranjaDao
    abstract fun puntoMedicionDao(): PuntoMedicionDao
    abstract fun medicionDao(): MedicionDao
    abstract fun alertaDao(): AlertaDao
    abstract fun accionCorrectivaDao(): AccionCorrectivaDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aquatrackx.db"
                ).build().also { instancia = it }
            }
    }
}
