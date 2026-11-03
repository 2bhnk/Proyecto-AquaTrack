package com.example.aquatrackx.service

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Recurso nativo 3/3: cámara del dispositivo (IE 2.4.1).
 * Genera un archivo temporal para capturar evidencia fotográfica del control o
 * de la acción correctiva (pregunta 23: fotografías como antecedente), expuesto
 * a la app de cámara mediante FileProvider (integración segura sin permisos de
 * almacenamiento).
 */
object CameraHelper {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    /** Crea el archivo destino de la foto y su Uri para el intent de cámara. */
    fun crearArchivoFoto(context: Context): Pair<File, Uri> {
        val directorio = File(context.cacheDir, "fotos").apply { mkdirs() }
        val nombre = "IMG_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) +
            ".jpg"
        val archivo = File(directorio, nombre)
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + AUTHORITY_SUFFIX,
            archivo
        )
        return archivo to uri
    }

    /** Ruta absoluta del archivo (para mostrar la miniatura tras la captura). */
    fun rutaDe(archivo: File): String = archivo.absolutePath
}
