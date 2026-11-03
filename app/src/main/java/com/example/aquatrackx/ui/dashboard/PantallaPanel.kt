package com.example.aquatrackx.ui.dashboard

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquatrackx.core.ChlorineRules
import com.example.aquatrackx.domain.model.EstadoCloro
import com.example.aquatrackx.ui.componentes.ChipEstado
import com.example.aquatrackx.ui.componentes.EncabezadoSeccion
import com.example.aquatrackx.ui.componentes.FilaDato
import com.example.aquatrackx.ui.componentes.GraficoLineas
import com.example.aquatrackx.ui.componentes.TarjetaAnimada
import com.example.aquatrackx.ui.theme.EstadoAdvertencia
import com.example.aquatrackx.ui.theme.EstadoCritico
import com.example.aquatrackx.ui.theme.EstadoNormal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Panel principal (pregunta 28): estado actual de los puntos, promedio /
 * máximo / mínimo de 24 h, gráfico histórico y alertas activas.
 */
@Composable
fun PantallaPanel(
    viewModel: DashboardViewModel,
    onIrAPunto: (Long) -> Unit
) {
    val estado by viewModel.uiState.collectAsState()

    if (estado.cargando) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Cargando panel…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
    ) {
        // ---- Encabezado con saludo y sincronización ----
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hola, ${estado.usuario?.nombre?.substringBefore(' ') ?: ""}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Panel de calidad del agua",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (estado.modoSinConexion) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = "Modo sin conexión",
                            tint = EstadoAdvertencia,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = viewModel::sincronizar,
                        enabled = !estado.sincronizando
                    ) {
                        if (estado.sincronizando) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Sync, contentDescription = "Sincronizar con la API")
                        }
                    }
                }
            }
        }

        // ---- Aviso de mediciones pendientes de sincronización (pregunta 12) ----
        if (estado.pendientesSincronizacion > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            text = "${estado.pendientesSincronizacion} medición(es) pendiente(s) de sincronizar",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // ---- Métricas 24 h (promedio, máximo, mínimo) ----
        item {
            TarjetaAnimada(indice = 0) {
                Column(Modifier.padding(16.dp)) {
                    EncabezadoSeccion(
                        titulo = "Resumen últimas 24 horas",
                        subtitulo = "Promedio, máximo y mínimo de cloro libre"
                    )
                    Spacer(Modifier.height(8.dp))
                    FilaDato(
                        etiqueta = "Promedio",
                        valor = estado.promedio24h?.let { "${ChlorineRules.formatoPpm(it)} ppm" } ?: "—",
                        colorValor = MaterialTheme.colorScheme.primary
                    )
                    FilaDato(
                        etiqueta = "Máximo",
                        valor = estado.maximo24h?.let { "${ChlorineRules.formatoPpm(it)} ppm" } ?: "—",
                        colorValor = EstadoCritico
                    )
                    FilaDato(
                        etiqueta = "Mínimo",
                        valor = estado.minimo24h?.let { "${ChlorineRules.formatoPpm(it)} ppm" } ?: "—",
                        colorValor = EstadoNormal
                    )
                }
            }
        }

        // ---- Gráfico histórico del promedio ----
        if (estado.serieHistorica.isNotEmpty()) {
            item {
                TarjetaAnimada(indice = 1) {
                    Column(Modifier.padding(16.dp)) {
                        EncabezadoSeccion(
                            titulo = "Evolución del promedio",
                            subtitulo = "Cloro libre por hora (24 h)"
                        )
                        Spacer(Modifier.height(12.dp))
                        GraficoLineas(
                            datos = estado.serieHistorica.map { it.y },
                            colorLinea = MaterialTheme.colorScheme.primary,
                            altura = 140.dp
                        )
                    }
                }
            }
        }

        // ---- Alertas activas ----
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EncabezadoSeccion(titulo = "Alertas activas")
                Text(
                    text = "${estado.criticasAbiertas} críticas / ${estado.alertasAbiertas.size} total",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (estado.criticasAbiertas > 0) EstadoCritico
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (estado.alertasAbiertas.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = EstadoNormal,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Sin alertas activas. Todos los puntos en rango.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            itemsIndexed(estado.alertasAbiertas) { indice, alerta ->
                TarjetaAnimada(
                    indice = indice + 2,
                    colorFondo = if (alerta.severidad == EstadoCloro.CRITICO)
                        MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = alerta.mensaje,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Valor: ${ChlorineRules.formatoPpm(alerta.valor)} ppm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        ChipEstado(estado = alerta.severidad.name)
                    }
                }
            }
        }

        // ---- Estado por punto de medición ----
        item {
            Spacer(Modifier.height(4.dp))
            EncabezadoSeccion(
                titulo = "Estado por punto",
                subtitulo = "${estado.estadoPuntos.size} puntos monitoreados"
            )
        }

        itemsIndexed(estado.estadoPuntos) { indice, item ->
            val colorEstado = when (item.ultima?.estado) {
                EstadoCloro.NORMAL -> EstadoNormal
                EstadoCloro.ADVERTENCIA -> EstadoAdvertencia
                EstadoCloro.CRITICO -> EstadoCritico
                null -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            TarjetaAnimada(indice = indice) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIrAPunto(item.punto.id) }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = item.punto.nombre,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${item.granja?.nombre ?: ""} · ${item.punto.codigo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = item.ultima?.let { ChlorineRules.formatoPpm(it.valor) } ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorEstado
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item.ultima?.let { ultima ->
                            Text(
                                text = "Actualizado ${formatearHora(ultima.fecha)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ChipEstado(estado = ultima.estado.name)
                        } ?: Text(
                            text = "Sin mediciones",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Barra proporcional al valor para lectura rápida.
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            item.ultima?.let { ChlorineRules.normalizarParaGauge(it.valor) } ?: 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = colorEstado,
                        trackColor = colorEstado.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Toca para ver detalle e histórico →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp)
                    )
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

private fun formatearHora(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
