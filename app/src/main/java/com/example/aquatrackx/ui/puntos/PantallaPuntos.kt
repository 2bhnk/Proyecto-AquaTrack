package com.example.aquatrackx.ui.puntos

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.example.aquatrackx.ui.componentes.TarjetaAnimada
import com.example.aquatrackx.ui.theme.EstadoAdvertencia
import com.example.aquatrackx.ui.theme.EstadoCritico
import com.example.aquatrackx.ui.theme.EstadoNormal

/**
 * Consulta jerárquica granja → puntos con su última medición y estado
 * (mejora esperada del caso: consultar niveles por granja y punto).
 */
@Composable
fun PantallaPuntos(
    viewModel: PuntosViewModel,
    onIrAPunto: (Long) -> Unit
) {
    val estado by viewModel.uiState.collectAsState()

    if (estado.cargando) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
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
        item {
            Text(
                text = "Granjas y puntos de medición",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Selecciona un punto para ver su estado e histórico",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
        }

        estado.agrupado.forEachIndexed { indiceGrupo, grupo ->
            item(key = "granja_${grupo.granja.id}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = grupo.granja.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${grupo.granja.codigo} · ${grupo.granja.ubicacion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${grupo.puntos.size} puntos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            itemsIndexed(grupo.puntos) { indice, item ->
                val colorEstado = when (item.ultima?.estado) {
                    EstadoCloro.NORMAL -> EstadoNormal
                    EstadoCloro.ADVERTENCIA -> EstadoAdvertencia
                    EstadoCloro.CRITICO -> EstadoCritico
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                TarjetaAnimada(indice = indice + indiceGrupo) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIrAPunto(item.punto.id) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.punto.nombre,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = item.punto.tipo.titulo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${item.punto.codigo} · ${item.punto.galpon}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            item.ultima?.let {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Última: ${ChlorineRules.formatoPpm(it.valor)} ppm",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colorEstado
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            item.ultima?.let { ChipEstado(estado = it.estado.name) }
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Ver detalle",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (estado.agrupado.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        text = "No hay puntos asignados a tu usuario.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
