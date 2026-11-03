package com.example.aquatrackx.ui.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquatrackx.data.local.UsuarioEntity
import com.example.aquatrackx.data.repository.AuthRepository
import com.example.aquatrackx.data.repository.MedicionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Sesión global: usuario autenticado, modo sin conexión y mediciones
 * pendientes de sincronización. Vive en un ViewModel de actividad para
 * sobrevivir a los cambios de pantalla.
 */
data class SesionUiState(
    val cargando: Boolean = true,
    val usuario: UsuarioEntity? = null,
    val modoSinConexion: Boolean = false
)

class SesionViewModel(
    private val authRepository: AuthRepository,
    private val medicionRepository: MedicionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SesionUiState())
    val uiState: StateFlow<SesionUiState> = _uiState.asStateFlow()

    private val _pendientes = MutableStateFlow(0)
    /** Mediciones registradas sin conexión y aún no sincronizadas (pregunta 12). */
    val pendientes: StateFlow<Int> = _pendientes.asStateFlow()

    init {
        restaurar()
        viewModelScope.launch {
            medicionRepository.pendientesDeSincronizacion().collect { total ->
                _pendientes.value = total
            }
        }
    }

    fun restaurar() {
        viewModelScope.launch {
            val usuario = authRepository.usuarioSesion()
            _uiState.value = SesionUiState(
                cargando = false,
                usuario = usuario,
                modoSinConexion = authRepository.modoSinConexion
            )
        }
    }

    fun cerrarSesion() {
        authRepository.cerrarSesion()
        _uiState.value = SesionUiState(cargando = false, usuario = null)
    }

    fun alternarModoConexion() {
        val nuevo = !_uiState.value.modoSinConexion
        authRepository.setModoSinConexion(nuevo)
        _uiState.update { it.copy(modoSinConexion = nuevo) }
    }

    /** Envía las mediciones pendientes a la API simulada (si hay conexión). */
    fun sincronizarPendientes() {
        if (_uiState.value.modoSinConexion) return
        viewModelScope.launch { medicionRepository.sincronizar() }
    }
}
