package com.example.aquatrackx

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.aquatrackx.data.AppContainer
import com.example.aquatrackx.data.local.AlertaEntity
import com.example.aquatrackx.service.NotificationHelper
import com.example.aquatrackx.ui.AppViewModelFactory
import com.example.aquatrackx.ui.alertas.AlertasViewModel
import com.example.aquatrackx.ui.alertas.PantallaAlertas
import com.example.aquatrackx.ui.dashboard.DashboardViewModel
import com.example.aquatrackx.ui.dashboard.PantallaPanel
import com.example.aquatrackx.ui.detalle.DetallePuntoViewModel
import com.example.aquatrackx.ui.detalle.PantallaDetallePunto
import com.example.aquatrackx.ui.historial.HistorialViewModel
import com.example.aquatrackx.ui.historial.PantallaHistorial
import com.example.aquatrackx.ui.login.LoginViewModel
import com.example.aquatrackx.ui.login.PantallaLogin
import com.example.aquatrackx.ui.navegacion.Rutas
import com.example.aquatrackx.ui.perfil.PantallaPerfil
import com.example.aquatrackx.ui.puntos.PantallaPuntos
import com.example.aquatrackx.ui.puntos.PuntosViewModel
import com.example.aquatrackx.ui.registro.PantallaRegistro
import com.example.aquatrackx.ui.registro.RegistroMedicionViewModel
import com.example.aquatrackx.ui.sesion.SesionViewModel
import com.example.aquatrackx.ui.theme.AquaTrackXTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as AquaTrackXApp
        setContent {
            AquaTrackXTheme {
                Aplicacion(container = app.container)
            }
        }
    }
}

/** Ítem de la navegación inferior (IE 2.1.1: navegación funcional y coherente). */
private data class ItemMenu(val ruta: String, val etiqueta: String, val icono: ImageVector)

private val ITEMS_MENU = listOf(
    ItemMenu(Rutas.PANEL, "Panel", Icons.Filled.Dashboard),
    ItemMenu(Rutas.PUNTOS, "Puntos", Icons.Filled.WaterDrop),
    ItemMenu(Rutas.ALERTAS, "Alertas", Icons.Filled.Notifications),
    ItemMenu(Rutas.HISTORIAL, "Histórico", Icons.Filled.History),
    ItemMenu(Rutas.PERFIL, "Perfil", Icons.Filled.Person)
)

/** Frecuencia de la API simulada del caso: una medición cada 5 minutos. */
private const val INTERVALO_SINCRONIZACION_MS = 5 * 60 * 1000L

@Composable
private fun Aplicacion(container: AppContainer) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val factory = remember { AppViewModelFactory(container) }
    val navController = rememberNavController()

    // Único ViewModel con alcance de actividad: debe sobrevivir a los cambios
    // de pantalla y al cierre de sesión.
    val sesionVm: SesionViewModel = viewModel(factory = factory, key = "sesion")

    // ---- Recurso nativo: permiso de notificaciones (Android 13+) ----
    val permisoNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Silencioso: sin permiso no se notifica (integración segura). */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.permisoConcedido(contexto)
        ) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Notifica las alertas críticas nuevas (recurso nativo: notificaciones). */
    suspend fun notificarCriticas(criticas: List<AlertaEntity>) {
        criticas.forEach { alerta ->
            val punto = container.catalogoRepository.punto(alerta.puntoId)
            NotificationHelper.notificarAlerta(contexto, alerta, punto)
        }
    }

    // ---- Sincronización periódica con la API simulada (cada 5 minutos) ----
    // Vive a nivel de actividad para que las notificaciones funcionen en
    // cualquier pantalla, no solo dentro del panel.
    LaunchedEffect(Unit) {
        while (true) {
            delay(INTERVALO_SINCRONIZACION_MS)
            try {
                notificarCriticas(container.medicionRepository.sincronizar())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Sin red / API no disponible: se reintenta en el próximo ciclo.
            }
        }
    }

    // ---- Pantalla de arranque mientras se restaura la sesión ----
    val sesion by sesionVm.uiState.collectAsState()
    if (sesion.cargando) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AquaTrackX",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator()
            }
        }
        return
    }

    val destinoInicial = if (sesion.usuario != null) Rutas.PANEL else Rutas.LOGIN
    val entrada by navController.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    val mostrarMenu = rutaActual != null && ITEMS_MENU.any { it.ruta == rutaActual }

    Scaffold(
        bottomBar = {
            if (mostrarMenu) {
                NavigationBar {
                    ITEMS_MENU.forEach { item ->
                        NavigationBarItem(
                            selected = rutaActual == item.ruta,
                            onClick = {
                                navController.navigate(item.ruta) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icono,
                                    contentDescription = item.etiqueta
                                )
                            },
                            label = { Text(item.etiqueta) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = destinoInicial,
            modifier = Modifier.padding(padding)
        ) {
            composable(Rutas.LOGIN) {
                // Se crea aquí para que cada ingreso parta con estado limpio.
                val loginVm: LoginViewModel = viewModel(factory = factory)
                PantallaLogin(viewModel = loginVm, onLoginExitoso = {
                    navController.navigate(Rutas.PANEL) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                })
            }

            composable(Rutas.PANEL) {
                // Creado al entrar a la pantalla: su init corre con sesión iniciada,
                // por lo que los datos se cargan correctamente.
                val dashboardVm: DashboardViewModel = viewModel(factory = factory)
                LaunchedEffect(dashboardVm) {
                    dashboardVm.onAlertasCriticas = { criticas ->
                        alcance.launch {
                            runCatching { notificarCriticas(criticas) }
                        }
                    }
                }
                PantallaPanel(
                    viewModel = dashboardVm,
                    onIrAPunto = { id -> navController.navigate(Rutas.detallePunto(id)) }
                )
            }

            composable(Rutas.PUNTOS) {
                val puntosVm: PuntosViewModel = viewModel(factory = factory)
                PantallaPuntos(
                    viewModel = puntosVm,
                    onIrAPunto = { id -> navController.navigate(Rutas.detallePunto(id)) }
                )
            }

            composable(Rutas.ALERTAS) {
                val alertasVm: AlertasViewModel = viewModel(factory = factory)
                PantallaAlertas(
                    viewModel = alertasVm,
                    onRegistrarPunto = { id ->
                        navController.navigate("${Rutas.REGISTRO}?puntoId=$id")
                    }
                )
            }

            composable(Rutas.HISTORIAL) {
                val historialVm: HistorialViewModel = viewModel(factory = factory)
                PantallaHistorial(viewModel = historialVm)
            }

            composable(Rutas.PERFIL) {
                PantallaPerfil(
                    viewModel = sesionVm,
                    onCerrarSesion = {
                        sesionVm.cerrarSesion()
                        navController.navigate(Rutas.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Rutas.DETALLE_PUNTO,
                arguments = listOf(
                    navArgument("puntoId") { type = NavType.LongType }
                )
            ) { entradaDetalle ->
                val puntoId = entradaDetalle.arguments?.getLong("puntoId") ?: 0L
                val detalleVm: DetallePuntoViewModel = viewModel(factory = factory)
                PantallaDetallePunto(
                    viewModel = detalleVm,
                    puntoId = puntoId,
                    onVolver = { navController.popBackStack() },
                    onRegistrar = {
                        navController.navigate("${Rutas.REGISTRO}?puntoId=$puntoId")
                    }
                )
            }

            composable(
                route = "${Rutas.REGISTRO}?puntoId={puntoId}",
                arguments = listOf(
                    navArgument("puntoId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { entradaRegistro ->
                val puntoId = entradaRegistro.arguments?.getLong("puntoId") ?: 0L
                val registroVm: RegistroMedicionViewModel = viewModel(factory = factory)
                PantallaRegistro(
                    viewModel = registroVm,
                    puntoId = puntoId,
                    onVolver = { navController.popBackStack() }
                )
            }
        }
    }
}
