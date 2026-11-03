package com.example.aquatrackx.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Recurso nativo 2/3: ubicación del dispositivo (IE 2.4.1).
 * Permite georreferenciar la medición manual desde terreno (pregunta 7: ubicación
 * del punto) mostrando las coordenadas como evidencia del control.
 *
 * Se usa el LocationManager del sistema (sin dependencias externas) y se integra
 * de forma segura: solo se consulta con el permiso concedido.
 */
object LocationHelper {

    val PERMISOS_UBICACION = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun permisoConcedido(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * Devuelve la última ubicación conocida o null si no hay permiso / dato.
     * En emuladores sin fix puede tardar; se resuelve con el mejor proveedor.
     */
    @SuppressLint("MissingPermission") // El llamador verifica el permiso antes.
    suspend fun ultimaUbicacion(context: Context): Location? {
        if (!permisoConcedido(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        return suspendCoroutine { continuation ->
            // Evita resolver dos veces (último conocido + actualización en curso).
            var resuelto = false
            fun resolver(locacion: Location?) {
                if (!resuelto) {
                    resuelto = true
                    continuation.resume(locacion)
                }
            }
            try {
                val proveedores = manager.getProviders(true)
                // Mejor precisión disponible entre los proveedores activos.
                val ultimo = proveedores
                    .mapNotNull { proveedor ->
                        runCatching { manager.getLastKnownLocation(proveedor) }.getOrNull()
                    }
                    .minByOrNull { it.accuracy }

                when {
                    ultimo != null -> resolver(ultimo)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                        // Sin último conocido: pide una actualización única.
                        manager.getCurrentLocation(
                            LocationManager.GPS_PROVIDER, null, context.mainExecutor
                        ) { locacion -> resolver(locacion) }

                    else -> resolver(null)
                }
            } catch (_: SecurityException) {
                resolver(null)
            }
        }
    }
}
