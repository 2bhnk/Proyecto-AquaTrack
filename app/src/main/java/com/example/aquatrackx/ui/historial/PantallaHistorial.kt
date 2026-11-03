package com.example.aquatrackx.ui.historial

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquatrackx.core.ChlorineRules
import com.example.aquatrackx.domain.model.EstadoCloro
import com.example.aquatrackx.domain.model.PeriodoHistorial
import com.example.aquatrackx.ui.componentes.ChipEstado
import com.example.aquatrackx.ui.componentes.EncabezadoSeccion
import com.example.aquatrackx.ui.componentes.TarjetaAnimada
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Histórico de mediciones (pregunta 27): filtros por período 24 h / 7 d / 30 d,
 * granja y punto, con contadores por estado y detección de pendientes de
 * sincronización.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorial(viewModel: HistorialViewModel) {
    val estado by viewModel.uiState.collectAsState()

    var menuGranja by remember { mutableStateOf(false) }
    var menuPunto by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de mediciones") },
                actions = {
                    if (estado.granjaFiltrada != null || estado.puntoFiltrado != null) {
                        TextButton(onClick = viewModel::limpiarFiltros) {
                            Icon(
                                Icons.Filled.FilterAltOff,
                                contentDescription = "Quitar filtros",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Limpiar")
                        }
                    }
                }
            )
        }
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

        val granjaActual = estado.granjaFiltrada
        val puntosVisibles = estado.puntos.filter {
            granjaActual == null || it.granjaId == granjaActual.id
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ---- Filtro por período (24 h / 7 días / 30 días) ----
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeriodoHistorial.entries.forEach { periodo ->
                        FilterChip(
                            selected = estado.periodo == periodo,
                            onClick = { viewModel.cambiarPeriodo(periodo) },
                            label = { Text(periodo.titulo) }
                        )
                    }
                }
            }

            // ---- Filtro por granja ----
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    OutlinedButton(onClick = { menuGranja = true }) {
                        Text(estado.granjaFiltrada?.nombre ?: "Todas las granjas")
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuGranja, onDismissRequest = { menuGranja = false }) {
                        DropdownMenuItem(
                            text = { Text("Todas las granjas") },
                            onClick = {
                                viewModel.filtrarPorGranja(null)
                                menuGranja = false
                            }
                        )
                        estado.granjas.forEach { granja ->
                            DropdownMenuItem(
                                text = { Text(granja.nombre) },
                                onClick = {
                                    viewModel.filtrarPorGranja(granja)
                                    menuGranja = false
                                }
                            )
                        }
                    }
                }
            }

            // ---- Filtro por punto ----
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    OutlinedButton(onClick = { menuPunto = true }) {
                        Text(estado.puntoFiltrado?.nombre ?: "Todos los puntos")
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuPunto, onDismissRequest = { menuPunto = false }) {
                        DropdownMenuItem(
                            text = { Text("Todos los puntos") },
                            onClick = {
                                viewModel.filtrarPorPunto(null)
                                menuPunto = false
                            }
                        )
                        puntosVisibles.forEach { punto ->
                            DropdownMenuItem(
                                text = { Text("${punto.nombre} · ${punto.codigo}") },
                                onClick = {
                                    viewModel.filtrarPorPunto(punto)
                                    menuPunto = false
                                }
                            )
                        }
                    }
                }
            }

            // ---- Contadores por estado ----
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ContadorEstado(
                        titulo = "Normal",
                        valor = estado.totalNormal,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    ContadorEstado(
                        titulo = "Advertencia",
                        valor = estado.totalAdvertencia,
                        color = Color(0xFFF9A825),
                        modifier = Modifier.weight(1f)
                    )
                    ContadorEstado(
                        titulo = "Crítico",
                        valor = estado.totalCritico,
                        color = Color(0xFFC62828),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                EncabezadoSeccion(
                    titulo = "Mediciones",
                    subtitulo = "${estado.mediciones.size} registro(s) en el período"
                )
            }

            if (estado.mediciones.isEmpty()) {
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
                                Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Sin mediciones para los filtros seleccionados.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            itemsIndexed(estado.mediciones) { indice, medicion ->
                val punto = estado.puntos.find { it.id == medicion.puntoId }
                TarjetaAnimada(indice = indice) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "${ChlorineRules.formatoPpm(medicion.valor)} ppm",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when (medicion.estado) {
                                    EstadoCloro.NORMAL -> Color(0xFF2E7D32)
                                    EstadoCloro.ADVERTENCIA -> Color(0xFFF9A825)
                                    EstadoCloro.CRITICO -> Color(0xFFC62828)
                                }
                            )
                            Text(
                                text = "${punto?.nombre ?: "Punto ${medicion.puntoId}"} · " +
                                    punto?.codigo.orEmpty(),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${formatear(medicion.fecha)} · ${medicion.origen.titulo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (medicion.pendienteSincronizacion) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CloudOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = "Pendiente de sincronización",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                        ChipEstado(estado = medicion.estado.name)
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ContadorEstado(
    titulo: String,
    valor: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$valor", style = MaterialTheme.typography.titleLarge, color = color)
            Text(text = titulo, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

private fun formatear(millis: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(millis))
