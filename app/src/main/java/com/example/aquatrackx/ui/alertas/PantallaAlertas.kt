package com.example.aquatrackx.ui.alertas

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquatrackx.core.ChlorineRules
import com.example.aquatrackx.core.validacion.Campos
import com.example.aquatrackx.data.local.AlertaEntity
import com.example.aquatrackx.domain.model.EstadoAlerta
import com.example.aquatrackx.ui.componentes.CampoTextoValidado
import com.example.aquatrackx.ui.componentes.ChipEstado
import com.example.aquatrackx.ui.componentes.EncabezadoSeccion
import com.example.aquatrackx.ui.componentes.TarjetaAnimada
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centro de alertas (preguntas 19-24): listado abierto/cerrado, detalle con
 * condiciones de cierre, registro de acción correctiva y cierre validado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAlertas(
    viewModel: AlertasViewModel,
    onRegistrarPunto: (Long) -> Unit
) {
    val estado by viewModel.uiState.collectAsState()

    // ---- Detalle de una alerta seleccionada ----
    estado.accionSeleccionada?.let { alerta ->
        DetalleAlerta(
            viewModel = viewModel,
            alerta = alerta,
            onVolver = viewModel::cerrarDetalle,
            onRegistrarPunto = onRegistrarPunto
        )
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Centro de alertas") }) }
    ) { padding ->
        if (estado.cargando) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Filtro abierto / cerrado (pregunta 21)
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = estado.soloAbiertas,
                        onClick = { viewModel.cambiarFiltro(true) },
                        label = { Text("Abiertas") }
                    )
                    FilterChip(
                        selected = !estado.soloAbiertas,
                        onClick = { viewModel.cambiarFiltro(false) },
                        label = { Text("Cerradas") }
                    )
                }
            }

            item {
                EncabezadoSeccion(
                    titulo = if (estado.soloAbiertas) "Alertas abiertas" else "Alertas cerradas",
                    subtitulo = "${estado.alertas.size} registro(s) · " +
                        "Solo supervisores y jefatura pueden cerrar"
                )
            }

            if (estado.alertas.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "No hay alertas en este filtro.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            itemsIndexed(estado.alertas) { indice, alerta ->
                TarjetaAnimada(indice = indice) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (alerta.severidad ==
                                        com.example.aquatrackx.domain.model.EstadoCloro.CRITICO
                                    ) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = alerta.tipo.titulo,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = alerta.mensaje,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Valor: ${ChlorineRules.formatoPpm(alerta.valor)} ppm · " +
                                    "Detectada ${formatear(alerta.creadaEn)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            ChipEstado(estado = alerta.severidad.name)
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(onClick = { viewModel.seleccionarAlerta(alerta) }) {
                                Text("Gestionar")
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** Detalle de la alerta: condiciones de cierre, acciones y cierre validado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetalleAlerta(
    viewModel: AlertasViewModel,
    alerta: AlertaEntity,
    onVolver: () -> Unit,
    onRegistrarPunto: (Long) -> Unit
) {
    val estado by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de alerta") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ---- Datos de la alerta ----
            item {
                TarjetaAnimada(indice = 0) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = alerta.tipo.titulo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            ChipEstado(estado = alerta.severidad.name)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = alerta.mensaje,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Valor detectado: ${ChlorineRules.formatoPpm(alerta.valor)} ppm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Estado: ${alerta.estado.titulo} · " +
                                "Detectada ${formatear(alerta.creadaEn)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        alerta.cerradaEn?.let {
                            Text(
                                text = "Cerrada ${formatear(it)} por ${alerta.cerradaPor ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ---- Condiciones de cierre (pregunta 22) ----
            if (alerta.estado == EstadoAlerta.ABIERTA) {
                item {
                    TarjetaAnimada(indice = 1) {
                        Column(Modifier.padding(16.dp)) {
                            EncabezadoSeccion(
                                titulo = "Condiciones de cierre",
                                subtitulo = "Ambas deben cumplirse para cerrar"
                            )
                            Spacer(Modifier.height(10.dp))
                            val condiciones = estado.condiciones
                            CondicionFila(
                                texto = "Acción correctiva registrada",
                                cumplida = condiciones?.tieneAccionCorrectiva == true
                            )
                            CondicionFila(
                                texto = "Medición posterior dentro del rango aceptable" +
                                    (condiciones?.valorVerificacion?.let {
                                        " (${ChlorineRules.formatoPpm(it)} ppm)"
                                    } ?: ""),
                                cumplida = condiciones?.medicionVerificada == true
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onRegistrarPunto(alerta.puntoId) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.AddTask,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Registrar medición en el punto")
                            }
                        }
                    }
                }
            }

            // ---- Acciones correctivas registradas ----
            item {
                EncabezadoSeccion(
                    titulo = "Acciones correctivas",
                    subtitulo = "${estado.accionesDeAlerta.size} registrada(s)"
                )
            }
            if (estado.accionesDeAlerta.isEmpty()) {
                item {
                    Text(
                        text = "Sin acciones registradas aún.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            itemsIndexed(estado.accionesDeAlerta) { indice, accion ->
                TarjetaAnimada(indice = indice + 2) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Gavel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = accion.responsable,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = accion.descripcion,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${accion.fecha.let { formatear(it) }} ${accion.hora} · " +
                                "Registrada por ${accion.creadaPor}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---- Formulario de acción correctiva ----
            if (alerta.estado == EstadoAlerta.ABIERTA && viewModel.puedeRegistrarAccion()) {
                if (!estado.formVisible) {
                    item {
                        Button(
                            onClick = viewModel::mostrarFormulario,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Registrar acción correctiva")
                        }
                    }
                } else {
                    item {
                        TarjetaAnimada(indice = 3) {
                            Column(Modifier.padding(16.dp)) {
                                EncabezadoSeccion(titulo = "Nueva acción correctiva")
                                Spacer(Modifier.height(10.dp))
                                CampoTextoValidado(
                                    valor = estado.descripcion,
                                    onValorChange = viewModel::onDescripcionChange,
                                    etiqueta = "Descripción de la acción",
                                    error = estado.errores[Campos.DESCIPCION],
                                    ayuda = "Ej: Dosificar cloro y verificar en 30 min",
                                    singleLine = false
                                )
                                Spacer(Modifier.height(8.dp))
                                CampoTextoValidado(
                                    valor = estado.responsable,
                                    onValorChange = viewModel::onResponsableChange,
                                    etiqueta = "Responsable",
                                    error = estado.errores[Campos.RESPONSABLE],
                                    ayuda = "Nombre de quien ejecuta la acción"
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = viewModel::guardarAccion,
                                        enabled = !estado.guardando
                                    ) {
                                        if (estado.guardando) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Text("Guardar acción")
                                        }
                                    }
                                    OutlinedButton(onClick = viewModel::limpiarMensajes) {
                                        Text("Cancelar")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---- Cierre de la alerta (solo supervisor / jefatura) ----
            if (alerta.estado == EstadoAlerta.ABIERTA && viewModel.puedeCerrarAlerta()) {
                item {
                    Button(
                        onClick = viewModel::cerrarAlerta,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = estado.condiciones?.tieneAccionCorrectiva == true &&
                            estado.condiciones?.medicionVerificada == true
                    ) {
                        Icon(Icons.Filled.TaskAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Cerrar alerta")
                    }
                    if (estado.condiciones?.let {
                            !it.tieneAccionCorrectiva || !it.medicionVerificada
                        } == true
                    ) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Registra la acción correctiva y una medición en rango " +
                                "para habilitar el cierre.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---- Mensajes de resultado ----
            estado.mensaje?.let { mensaje ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = mensaje,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            estado.error?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun CondicionFila(texto: String, cumplida: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            if (cumplida) Icons.Filled.CheckCircle else Icons.Filled.Close,
            contentDescription = null,
            tint = if (cumplida) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Text(text = texto, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatear(millis: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(millis))
