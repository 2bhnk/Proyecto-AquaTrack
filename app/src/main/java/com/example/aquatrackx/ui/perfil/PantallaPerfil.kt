package com.example.aquatrackx.ui.perfil

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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aquatrackx.ui.componentes.ChipEstado
import com.example.aquatrackx.ui.componentes.EncabezadoSeccion
import com.example.aquatrackx.ui.componentes.TarjetaAnimada
import com.example.aquatrackx.ui.sesion.SesionViewModel

/**
 * Perfil de usuario y sesión (IE 2.1.1: navegación coherente con destino propio):
 * identidad y rol, control del modo sin conexión con sincronización de
 * mediciones pendientes, resumen de los recursos nativos y cierre de sesión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPerfil(
    viewModel: SesionViewModel,
    onCerrarSesion: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    val pendientes by viewModel.pendientes.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Perfil y sesión") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ---- Identidad del usuario ----
            estado.usuario?.let { usuario ->
                item {
                    TarjetaAnimada(indice = 0) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .padding(0.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text(
                                    text = usuario.nombre,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = usuario.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ChipEstado(estado = usuario.rol.name)
                                    AssistChip(
                                        onClick = { },
                                        enabled = false,
                                        label = { Text("Rol principal") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.VerifiedUser,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ---- Datos del usuario ----
                item {
                    TarjetaAnimada(indice = 1) {
                        Column(Modifier.padding(16.dp)) {
                            EncabezadoSeccion(titulo = "Datos del usuario")
                            Spacer(Modifier.height(8.dp))
                            FilaDatoPerfil(Icons.Filled.Badge, "Nombre", usuario.nombre)
                            FilaDatoPerfil(Icons.Filled.Email, "Correo", usuario.email)
                            FilaDatoPerfil(
                                Icons.Filled.Work,
                                "Rol asignado",
                                usuario.rol.titulo
                            )
                            FilaDatoPerfil(
                                Icons.Filled.Person,
                                "Granjas asignadas",
                                if (usuario.granjaIds.isEmpty()) "Todas (jefatura)"
                                else "${usuario.granjaIds.size} granja(s)"
                            )
                        }
                    }
                }
            }

            // ---- Modo de conexión y sincronización ----
            item {
                TarjetaAnimada(indice = 2) {
                    Column(Modifier.padding(16.dp)) {
                        EncabezadoSeccion(
                            titulo = "Modo de conexión",
                            subtitulo = "Simula la operación sin red (pregunta 12)"
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (estado.modoSinConexion) Icons.Filled.CloudOff
                                    else Icons.Filled.CloudDone,
                                    contentDescription = null,
                                    tint = if (estado.modoSinConexion)
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = if (estado.modoSinConexion) "Sin conexión"
                                        else "Conectado",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (estado.modoSinConexion)
                                            "Las mediciones quedan guardadas localmente"
                                        else "Sincronización automática con la API",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = !estado.modoSinConexion,
                                onCheckedChange = { viewModel.alternarModoConexion() }
                            )
                        }

                        if (pendientes > 0) {
                            Spacer(Modifier.height(12.dp))
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
                                    Icon(
                                        Icons.Filled.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "$pendientes medición(es) pendiente(s) " +
                                            "de sincronizar",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = viewModel::sincronizarPendientes,
                                enabled = !estado.modoSinConexion,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sincronizar ahora")
                            }
                        }
                    }
                }
            }

            // ---- Recursos nativos del dispositivo ----
            item {
                TarjetaAnimada(indice = 3) {
                    Column(Modifier.padding(16.dp)) {
                        EncabezadoSeccion(
                            titulo = "Recursos nativos utilizados",
                            subtitulo = "Tres recursos del dispositivo (IE 2.4.1)"
                        )
                        Spacer(Modifier.height(10.dp))
                        FilaRecurso(
                            Icons.Filled.PhotoCamera,
                            "Cámara",
                            "Evidencia fotográfica al registrar una medición"
                        )
                        FilaRecurso(
                            Icons.Filled.LocationOn,
                            "Ubicación",
                            "Georreferencia GPS de cada medición de terreno"
                        )
                        FilaRecurso(
                            Icons.Filled.Notifications,
                            "Notificaciones",
                            "Alertas críticas de cloro en el dispositivo"
                        )
                    }
                }
            }

            // ---- Cerrar sesión ----
            item {
                Button(
                    onClick = onCerrarSesion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión")
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun FilaDatoPerfil(icono: ImageVector, titulo: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icono,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FilaRecurso(icono: ImageVector, titulo: String, detalle: String) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icono,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Column {
            Text(text = titulo, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
