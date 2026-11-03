package com.example.aquatrackx.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquatrackx.core.ChlorineRules
import com.example.aquatrackx.domain.model.EstadoCloro
import com.example.aquatrackx.domain.model.PeriodoHistorial
import com.example.aquatrackx.ui.componentes.ChipEstado
import com.example.aquatrackx.ui.componentes.EncabezadoSeccion
import com.example.aquatrackx.ui.componentes.FilaDato
import com.example.aquatrackx.ui.componentes.GraficoLineas
import com.example.aquatrackx.ui.componentes.MedidorCloro
import com.example.aquatrackx.ui.componentes.TarjetaAnimada
import com.example.aquatrackx.ui.theme.EstadoAdvertencia
import com.example.aquatrackx.ui.theme.EstadoCritico
import com.example.aquatrackx.ui.theme.EstadoNormal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Detalle del punto: medidor animado, estado, alerta abierta, resumen del
 * período, gráfico con filtro 24 h / 7 días / 30 días e histórico de valores
 * (preguntas 7 y 27).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetallePunto(
    viewModel: DetallePuntoViewModel,
    puntoId: Long,
    onVolver: () -> Unit,
    onRegistrar: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(puntoId) { viewModel.cargar(puntoId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(estado.punto?.nombre ?: "Detalle del punto")
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onRegistrar) {
                        Icon(Icons.Filled.Add, contentDescription = "Registrar medición")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (estado.cargando || estado.punto == null) {
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

        val punto = estado.punto!!

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
            // ---- Medidor animado + identificación ----
            item {
                TarjetaAnimada(indice = 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${punto.codigo} · ${punto.galpon}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        MedidorCloro(
                            valor = estado.ultima?.valor ?: 0.0,
                            estado = estado.ultima?.estado?.name ?: "—"
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Rango normal: ${viewModel.rangoNormal()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        estado.ultima?.let { ultima ->
                            Text(
                                text = "Actualizado ${formatearFechaHora(ultima.fecha)} · ${ultima.origen.titulo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ---- Alerta abierta del punto (si existe) ----
            estado.alertaAbierta?.let { alerta ->
                item {
                    val esCritica = alerta.severidad == EstadoCloro.CRITICO
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (esCritica)
                                MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = if (esCritica) MaterialTheme.colorScheme.error
                                else EstadoAdvertencia,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Alerta ${alerta.severidad.titulo.lowercase()} · ${alerta.tipo.titulo}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = alerta.mensaje,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            ChipEstado(estado = alerta.severidad.name)
                        }
                    }
                }
            }

            // ---- Resumen del período ----
            item {
                TarjetaAnimada(indice = 1) {
                    Column(Modifier.padding(16.dp)) {
                        EncabezadoSeccion(
                            titulo = "Resumen · ${estado.periodo.titulo}",
                            subtitulo = "${estado.historial.size} mediciones"
                        )
                        Spacer(Modifier.height(8.dp))
                        FilaDato(
                            etiqueta = "Promedio",
                            valor = estado.promedio?.let { "${ChlorineRules.formatoPpm(it)} ppm" } ?: "—",
                            colorValor = MaterialTheme.colorScheme.primary
                        )
                        FilaDato(
                            etiqueta = "Máximo",
                            valor = estado.maximo?.let { "${ChlorineRules.formatoPpm(it)} ppm" } ?: "—",
                            colorValor = EstadoCritico
                        )
                        FilaDato(
                            etiqueta = "Mínimo",
                            valor = estado.minimo?.let { "${ChlorineRules.formatoPpm(it)} ppm" } ?: "—",
                            colorValor = EstadoNormal
                        )
                    }
                }
            }

            // ---- Filtro de período + gráfico (pregunta 27) ----
            item {
                TarjetaAnimada(indice = 2) {
                    Column(Modifier.padding(16.dp)) {
                        EncabezadoSeccion(titulo = "Histórico")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PeriodoHistorial.entries.forEach { periodo ->
                                FilterChip(
                                    selected = estado.periodo == periodo,
                                    onClick = { viewModel.cambiarPeriodo(periodo) },
                                    label = { Text(periodo.titulo) }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (estado.serie.size >= 2) {
                            val colorGrafico = when {
                                (estado.maximo ?: 0.0) > 6.0 ||
                                    (estado.minimo ?: 99.0) < 2.0 -> EstadoCritico
                                (estado.maximo ?: 0.0) > 5.0 ||
                                    (estado.minimo ?: 99.0) < 3.0 -> EstadoAdvertencia
                                else -> EstadoNormal
                            }
                            GraficoLineas(
                                datos = estado.serie,
                                colorLinea = colorGrafico,
                                altura = 150.dp
                            )
                        } else {
                            Text(
                                text = "Sin datos suficientes para el gráfico.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ---- Tabla de mediciones recientes ----
            item {
                EncabezadoSeccion(titulo = "Mediciones recientes")
            }

            itemsIndexed(estado.historial.take(30)) { indice, medicion ->
                TarjetaAnimada(indice = indice + 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "${ChlorineRules.formatoPpm(medicion.valor)} ppm",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = when (medicion.estado) {
                                    EstadoCloro.NORMAL -> EstadoNormal
                                    EstadoCloro.ADVERTENCIA -> EstadoAdvertencia
                                    EstadoCloro.CRITICO -> EstadoCritico
                                }
                            )
                            Text(
                                text = "${formatearFechaHora(medicion.fecha)} · ${medicion.origen.titulo}" +
                                    (medicion.registradoPor?.let { " · $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ChipEstado(estado = medicion.estado.name)
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

private fun formatearFechaHora(millis: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(millis))
