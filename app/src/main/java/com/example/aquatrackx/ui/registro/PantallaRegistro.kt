package com.example.aquatrackx.ui.registro

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquatrackx.core.ChlorineRules
import com.example.aquatrackx.core.validacion.Campos
import com.example.aquatrackx.service.CameraHelper
import com.example.aquatrackx.service.LocationHelper
import com.example.aquatrackx.ui.componentes.CampoTextoValidado
import com.example.aquatrackx.ui.componentes.EncabezadoSeccion
import com.example.aquatrackx.ui.componentes.MedidorCloro
import com.example.aquatrackx.ui.componentes.TarjetaAnimada
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * Registro manual de medición (pregunta 11) con validación por campo,
 * previsualización animada del medidor y uso de recursos nativos:
 * cámara (evidencia fotográfica) y ubicación (georreferencia).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRegistro(
    viewModel: RegistroMedicionViewModel,
    puntoId: Long,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    var menuPuntos by remember { mutableStateOf(false) }
    var archivoFoto by remember { mutableStateOf<File?>(null) }

    // ---- Recurso nativo: cámara (evidencia fotográfica) ----
    val tomarFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) archivoFoto?.let { viewModel.onFotoCapturada(it.absolutePath) }
    }
    val permisoCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            val (archivo, uri) = CameraHelper.crearArchivoFoto(contexto)
            archivoFoto = archivo
            tomarFoto.launch(uri)
        }
    }

    // ---- Recurso nativo: ubicación (georreferencia de la medición) ----
    val permisoUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        if (resultados.values.any { it }) {
            obtenerUbicacion(contexto, alcance, viewModel::onUbicacionObtenida)
        }
    }

    fun pedirUbicacion() {
        if (LocationHelper.permisoConcedido(contexto)) {
            obtenerUbicacion(contexto, alcance, viewModel::onUbicacionObtenida)
        } else {
            permisoUbicacion.launch(LocationHelper.PERMISOS_UBICACION)
        }
    }

    LaunchedEffect(puntoId) { viewModel.precargarPunto(puntoId) }

    // Previsualización del estado según el valor tipeado (sin guardar).
    val valorPreview = estado.valorTexto.replace(',', '.').toDoubleOrNull() ?: 0.0
    val puntoActual = estado.puntoSeleccionado
    val hayValor = estado.valorTexto.isNotBlank() &&
        estado.valorTexto.replace(',', '.').toDoubleOrNull() != null
    val estadoPreview = if (puntoActual != null && hayValor) {
        ChlorineRules.clasificar(
            valorPreview,
            puntoActual.normalMin, puntoActual.normalMax,
            puntoActual.criticoMin, puntoActual.criticoMax
        ).name
    } else {
        "—"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar medición") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (estado.puntos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Cargando puntos de medición…")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ---- Selección del punto (jerarquía granja → punto) ----
            item {
                TarjetaAnimada(indice = 0) {
                    Column(Modifier.padding(16.dp)) {
                        EncabezadoSeccion(
                            titulo = "Punto de medición",
                            subtitulo = "Granja, galpón y tipo de toma"
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = { menuPuntos = true }) {
                                Text(
                                    text = puntoActual?.let { "${it.nombre} · ${it.codigo}" }
                                        ?: "Seleccionar punto",
                                    maxLines = 1
                                )
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = menuPuntos,
                                onDismissRequest = { menuPuntos = false }
                            ) {
                                estado.puntos.forEach { punto ->
                                    DropdownMenuItem(
                                        text = { Text("${punto.nombre} · ${punto.codigo}") },
                                        onClick = {
                                            viewModel.seleccionarPunto(punto)
                                            menuPuntos = false
                                        }
                                    )
                                }
                            }
                        }
                        estado.errores[Campos.PUNTO]?.let { error ->
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "⚠ $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        puntoActual?.let { punto ->
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "${punto.tipo.titulo} · ${punto.galpon} · ${punto.ubicacion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Rango normal: ${ChlorineRules.formatoPpm(punto.normalMin)} – " +
                                    "${ChlorineRules.formatoPpm(punto.normalMax)} ppm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ---- Valor medido con validación por campo ----
            item {
                TarjetaAnimada(indice = 1) {
                    Column(Modifier.padding(16.dp)) {
                        EncabezadoSeccion(
                            titulo = "Valor medido",
                            subtitulo = "Cloro libre en ppm, máximo 2 decimales"
                        )
                        Spacer(Modifier.height(10.dp))
                        CampoTextoValidado(
                            valor = estado.valorTexto,
                            onValorChange = viewModel::onValorChange,
                            etiqueta = "Cloro libre (ppm)",
                            error = estado.errores[Campos.VALOR],
                            esNumerico = true,
                            ayuda = "Ej: 3,50",
                            icono = { Icon(Icons.Filled.Science, contentDescription = null) }
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { },
                                enabled = false,
                                label = { Text("Lectura manual") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Sensors,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                            TextButton(onClick = viewModel::simularLectura) {
                                Text("Simular lectura API")
                            }
                        }
                    }
                }
            }

            // ---- Previsualización animada del medidor ----
            item {
                TarjetaAnimada(indice = 2) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Previsualización",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        MedidorCloro(valor = valorPreview, estado = estadoPreview)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Se clasifica según los rangos del punto seleccionado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---- Evidencia fotográfica (cámara) ----
            item {
                TarjetaAnimada(indice = 3) {
                    Column(Modifier.padding(16.dp)) {
                        EncabezadoSeccion(
                            titulo = "Evidencia fotográfica",
                            subtitulo = "Recurso nativo: cámara del dispositivo"
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(onClick = { permisoCamara.launch(Manifest.permission.CAMERA) }) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Tomar foto")
                            }
                            estado.fotoRuta?.let { ruta ->
                                val miniatura = remember(ruta) {
                                    val opciones = BitmapFactory.Options().apply { inSampleSize = 4 }
                                    runCatching { BitmapFactory.decodeFile(ruta, opciones) }
                                        .getOrNull()
                                }
                                miniatura?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Evidencia fotográfica",
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(MaterialTheme.shapes.small)
                                    )
                                }
                                Text(
                                    text = "Adjunta: ${File(ruta).name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } ?: Text(
                                text = "Sin evidencia adjunta",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ---- Ubicación (georreferencia) ----
            item {
                TarjetaAnimada(indice = 4) {
                    Column(Modifier.padding(16.dp)) {
                        EncabezadoSeccion(
                            titulo = "Ubicación",
                            subtitulo = "Recurso nativo: GPS del dispositivo"
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                if (estado.ubicacionTexto != null) Icons.Filled.LocationOn
                                else Icons.Filled.LocationOff,
                                contentDescription = null,
                                tint = if (estado.ubicacionTexto != null)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = estado.ubicacionTexto ?: "Sin georreferencia capturada",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { pedirUbicacion() }) {
                            Text("Obtener ubicación actual")
                        }
                    }
                }
            }

            // ---- Aviso / resultado del guardado ----
            estado.aviso?.let { aviso ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = aviso,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onVolver) { Text("Volver") }
                                OutlinedButton(onClick = viewModel::reiniciar) {
                                    Text("Otra medición")
                                }
                            }
                        }
                    }
                }
            }

            // ---- Guardar ----
            item {
                Button(
                    onClick = viewModel::guardar,
                    enabled = !estado.guardando && estado.aviso == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (estado.guardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardar medición", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** Consulta la última ubicación conocida y la reporta en formato texto. */
private fun obtenerUbicacion(
    contexto: Context,
    alcance: CoroutineScope,
    onResultado: (String) -> Unit
) {
    alcance.launch {
        val ubicacion = LocationHelper.ultimaUbicacion(contexto)
        onResultado(
            ubicacion?.let {
                String.format(Locale.US, "%.5f, %.5f", it.latitude, it.longitude)
            } ?: "Sin señal de GPS en el dispositivo"
        )
    }
}
