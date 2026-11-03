package com.example.aquatrackx.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.aquatrackx.R
import com.example.aquatrackx.core.ChlorineRules
import com.example.aquatrackx.data.local.AlertaEntity
import com.example.aquatrackx.data.local.PuntoMedicionEntity

/**
 * Recurso nativo 1/3: notificaciones locales del dispositivo (IE 2.4.1).
 * Se usan para destacar de inmediato las alertas críticas (preguntas 20 y 26).
 */
object NotificationHelper {

    const val CHANNEL_ID = "alertas_cloro"

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Alertas de cloro",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas críticas y de advertencia de cloro libre"
            }
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }
    }

    /** ¿Concede el usuario el permiso de notificaciones (Android 13+)? */
    fun permisoConcedido(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * Muestra la notificación de una alerta crítica. Silenciosamente omite el
     * envío si el permiso no fue concedido (integración segura, IE 2.4.1).
     */
    fun notificarAlerta(context: Context, alerta: AlertaEntity, punto: PuntoMedicionEntity?) {
        if (!permisoConcedido(context)) return

        val titulo = when (alerta.severidad) {
            com.example.aquatrackx.domain.model.EstadoCloro.CRITICO -> "⚠ Cloro crítico"
            else -> "Cloro fuera de rango"
        }
        val cuerpo = buildString {
            append(alerta.mensaje)
            punto?.let { append(" · ${it.codigo}") }
            append(" · ${ChlorineRules.formatoPpm(alerta.valor)} ppm")
        }

        val notificacion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(
                if (alerta.severidad == com.example.aquatrackx.domain.model.EstadoCloro.CRITICO)
                    NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .build()

        // ID estable por alerta: actualiza en lugar de apilar notificaciones.
        NotificationManagerCompat.from(context).notify(alerta.id.toInt(), notificacion)
    }
}
