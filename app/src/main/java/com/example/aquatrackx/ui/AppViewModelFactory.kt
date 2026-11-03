package com.example.aquatrackx.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.aquatrackx.AquaTrackXApp
import com.example.aquatrackx.data.AppContainer
import com.example.aquatrackx.ui.alertas.AlertasViewModel
import com.example.aquatrackx.ui.dashboard.DashboardViewModel
import com.example.aquatrackx.ui.detalle.DetallePuntoViewModel
import com.example.aquatrackx.ui.historial.HistorialViewModel
import com.example.aquatrackx.ui.login.LoginViewModel
import com.example.aquatrackx.ui.puntos.PuntosViewModel
import com.example.aquatrackx.ui.registro.RegistroMedicionViewModel
import com.example.aquatrackx.ui.sesion.SesionViewModel

/**
 * Factory de ViewModels: inyecta el [AppContainer] sin frameworks externos.
 * Cada ViewModel recibe solo los repositorios que necesita (baja acoplamiento).
 */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(container.authRepository) as T

            modelClass.isAssignableFrom(SesionViewModel::class.java) ->
                SesionViewModel(
                    container.authRepository,
                    container.medicionRepository
                ) as T

            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(
                    container.authRepository,
                    container.catalogoRepository,
                    container.medicionRepository,
                    container.alertaRepository
                ) as T

            modelClass.isAssignableFrom(PuntosViewModel::class.java) ->
                PuntosViewModel(
                    container.authRepository,
                    container.catalogoRepository,
                    container.medicionRepository
                ) as T

            modelClass.isAssignableFrom(DetallePuntoViewModel::class.java) ->
                DetallePuntoViewModel(
                    container.authRepository,
                    container.catalogoRepository,
                    container.medicionRepository,
                    container.alertaRepository
                ) as T

            modelClass.isAssignableFrom(RegistroMedicionViewModel::class.java) ->
                RegistroMedicionViewModel(
                    container.authRepository,
                    container.catalogoRepository,
                    container.medicionRepository
                ) as T

            modelClass.isAssignableFrom(AlertasViewModel::class.java) ->
                AlertasViewModel(
                    container.authRepository,
                    container.catalogoRepository,
                    container.alertaRepository
                ) as T

            modelClass.isAssignableFrom(HistorialViewModel::class.java) ->
                HistorialViewModel(
                    container.authRepository,
                    container.catalogoRepository,
                    container.medicionRepository
                ) as T

            else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
        }
    }
}
