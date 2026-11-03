package com.example.aquatrackx.ui.alertas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquatrackx.core.validacion.AccionCorrectivaValidator
import com.example.aquatrackx.core.validacion.Campos
import com.example.aquatrackx.data.local.AccionCorrectivaEntity
import com.example.aquatrackx.data.local.AlertaEntity
import com.example.aquatrackx.data.local.UsuarioEntity
import com.example.aquatrackx.data.repository.AlertaRepository
import com.example.aquatrackx.data.repository.AuthRepository
import com.example.aquatrackx.data.repository.CatalogoRepository
import com.example.aquatrackx.data.repository.CondicionCierre
import com.example.aquatrackx.data.repository.ResultadoCierre
import com.example.aquatrackx.domain.model.EstadoAlerta
import com.example.aquatrackx.domain.model.Rol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Alertas y desviaciones (preguntas 19-24): listado con filtro abierto/cerrado,
 * detalle con condiciones de cierre, registro de acción correctiva y cierre
 * validado por un usuario autorizado.
 */
data class AlertasUiState(
    val cargando: Boolean = true,
    val usuario: UsuarioEntity? = null,
    val soloAbiertas: Boolean = true,
    val alertas: List<AlertaEntity> = emptyList(),
    val accionSeleccionada: AlertaEntity? = null,
    val accionesDeAlerta: List<AccionCorrectivaEntity> = emptyList(),
    val condiciones: CondicionCierre? = null,
    /** Formulario de acción correctiva. */
    val formVisible: Boolean = false,
    val descripcion: String = "",
    val responsable: String = "",
    val errores: Map<String, String> = emptyMap(),
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null
)

class AlertasViewModel(
    private val authRepository: AuthRepository,
    private val catalogoRepository: CatalogoRepository,
    private val alertaRepository: AlertaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertasUiState())
    val uiState: StateFlow<AlertasUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val usuario = authRepository.usuarioSesion() ?: return@launch
            _uiState.update { it.copy(usuario = usuario) }

            // Filtra las alertas según los puntos visibles para el rol (pregunta 14).
            catalogoRepository.puntosVisibles(usuario).collect { puntos ->
                val ids = puntos.map { it.id }.toSet()
                val filtro = if (_uiState.value.soloAbiertas) EstadoAlerta.ABIERTA
                else EstadoAlerta.CERRADA
                alertaRepository.porEstado(filtro).collect { todas ->
                    val visibles = if (usuario.granjaIds.isEmpty()) todas
                    else todas.filter { it.puntoId in ids }
                    _uiState.update {
                        it.copy(alertas = visibles, cargando = false)
                    }
                }
            }
        }
    }

    fun cambiarFiltro(soloAbiertas: Boolean) {
        _uiState.update { it.copy(soloAbiertas = soloAbiertas) }
        viewModelScope.launch {
            val usuario = _uiState.value.usuario ?: return@launch
            catalogoRepository.puntosVisibles(usuario).collect { puntos ->
                val ids = puntos.map { it.id }.toSet()
                val filtro = if (soloAbiertas) EstadoAlerta.ABIERTA else EstadoAlerta.CERRADA
                alertaRepository.porEstado(filtro).collect { todas ->
                    val visibles = if (usuario.granjaIds.isEmpty()) todas
                    else todas.filter { it.puntoId in ids }
                    _uiState.update { it.copy(alertas = visibles) }
                }
            }
        }
    }

    fun seleccionarAlerta(alerta: AlertaEntity) {
        _uiState.update {
            it.copy(
                accionSeleccionada = alerta,
                formVisible = false,
                errores = emptyMap(),
                mensaje = null,
                error = null
            )
        }
        viewModelScope.launch {
            alertaRepository.accionesDeAlerta(alerta.id).collect { acciones ->
                _uiState.update { it.copy(accionesDeAlerta = acciones) }
            }
            val condiciones = alertaRepository.condicionesDeCierre(alerta)
            _uiState.update { it.copy(condiciones = condiciones) }
        }
    }

    fun cerrarDetalle() {
        _uiState.update { it.copy(accionSeleccionada = null, formVisible = false) }
    }

    fun mostrarFormulario() {
        val usuario = _uiState.value.usuario
        val responsableSugerido = usuario?.nombre ?: ""
        _uiState.update {
            it.copy(
                formVisible = true,
                responsable = if (it.responsable.isBlank()) responsableSugerido
                else it.responsable,
                errores = emptyMap(),
                mensaje = null,
                error = null
            )
        }
    }

    fun onDescripcionChange(valor: String) {
        _uiState.update {
            it.copy(descripcion = valor, errores = it.errores - Campos.DESCIPCION)
        }
    }

    fun onResponsableChange(valor: String) {
        _uiState.update {
            it.copy(responsable = valor, errores = it.errores - Campos.RESPONSABLE)
        }
    }

    fun guardarAccion() {
        val estado = _uiState.value
        val alerta = estado.accionSeleccionada ?: return
        val validacion = AccionCorrectivaValidator.validar(estado.descripcion, estado.responsable)
        if (!validacion.esValido) {
            _uiState.update { it.copy(errores = validacion.errores) }
            return
        }

        _uiState.update { it.copy(guardando = true, errores = emptyMap()) }
        viewModelScope.launch {
            val creador = estado.usuario?.email ?: return@launch
            alertaRepository.registrarAccion(
                alerta = alerta,
                descripcion = estado.descripcion,
                responsable = estado.responsable,
                creadaPor = creador
            )
            // Recarga condiciones y acciones tras el alta.
            val acciones = alertaRepository.accionesDeAlerta(alerta.id)
            val condiciones = alertaRepository.condicionesDeCierre(alerta)
            acciones.collect { lista ->
                val cond = alertaRepository.condicionesDeCierre(alerta)
                _uiState.update {
                    it.copy(
                        guardando = false,
                        formVisible = false,
                        descripcion = "",
                        accionesDeAlerta = lista,
                        condiciones = cond,
                        mensaje = "Acción correctiva registrada"
                    )
                }
            }
            _uiState.update { it.copy(condiciones = condiciones) }
        }
    }

    fun cerrarAlerta() {
        val estado = _uiState.value
        val alerta = estado.accionSeleccionada ?: return
        val usuario = estado.usuario ?: return

        viewModelScope.launch {
            when (val resultado = alertaRepository.cerrarAlerta(alerta, usuario)) {
                is ResultadoCierre.Exitoso -> {
                    _uiState.update {
                        it.copy(
                            mensaje = "Alerta cerrada correctamente",
                            error = null,
                            accionSeleccionada = null
                        )
                    }
                    // Refresca condiciones de la nueva vista.
                    cambiarFiltro(estado.soloAbiertas)
                }
                is ResultadoCierre.Error -> _uiState.update {
                    it.copy(error = resultado.mensaje, mensaje = null)
                }
            }
        }
    }

    fun puedeRegistrarAccion(): Boolean =
        _uiState.value.usuario?.rol in listOf(Rol.OPERARIO, Rol.SUPERVISOR)

    fun puedeCerrarAlerta(): Boolean =
        _uiState.value.usuario?.rol in listOf(Rol.SUPERVISOR, Rol.JEFATURA)

    fun limpiarMensajes() {
        _uiState.update { it.copy(mensaje = null, error = null) }
    }
}
