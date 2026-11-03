package com.example.aquatrackx.ui.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquatrackx.core.validacion.Campos
import com.example.aquatrackx.core.validacion.MedicionValidator
import com.example.aquatrackx.data.local.MedicionEntity
import com.example.aquatrackx.data.local.PuntoMedicionEntity
import com.example.aquatrackx.data.repository.AuthRepository
import com.example.aquatrackx.data.repository.CatalogoRepository
import com.example.aquatrackx.data.repository.MedicionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Formulario de registro manual de medición (pregunta 11).
 * La validación viene de [MedicionValidator] (desacoplada, IE 2.2.1); aquí solo
 * se orquesta el estado y se exponen las consecuencias (alerta, sincronización).
 */
data class RegistroUiState(
    val puntos: List<PuntoMedicionEntity> = emptyList(),
    val puntoSeleccionado: PuntoMedicionEntity? = null,
    val valorTexto: String = "",
    val errores: Map<String, String> = emptyMap(),
    val guardando: Boolean = false,
    val exitoso: Boolean = false,
    val medicionGuardada: MedicionEntity? = null,
    val aviso: String? = null,
    /** Evidencia fotográfica y georreferencia (recursos nativos, IE 2.4.1). */
    val fotoRuta: String? = null,
    val ubicacionTexto: String? = null
)

class RegistroMedicionViewModel(
    private val authRepository: AuthRepository,
    private val catalogoRepository: CatalogoRepository,
    private val medicionRepository: MedicionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    /** Punto pedido por navegación (detalle / alerta); se aplica al cargar el catálogo. */
    private var puntoPendiente: Long? = null

    init {
        viewModelScope.launch {
            val usuario = authRepository.usuarioSesion() ?: return@launch
            catalogoRepository.puntosVisibles(usuario).collect { puntos ->
                _uiState.update { it.copy(puntos = puntos) }
                puntoPendiente?.let { pendiente ->
                    puntos.find { it.id == pendiente }?.let { punto ->
                        if (_uiState.value.puntoSeleccionado?.id != punto.id) {
                            seleccionarPunto(punto)
                        }
                    }
                }
            }
        }
    }

    /** Preselecciona un punto al abrir el formulario desde otra pantalla. */
    fun precargarPunto(puntoId: Long?) {
        if (puntoId == null || puntoId <= 0L) return
        puntoPendiente = puntoId
        val punto = _uiState.value.puntos.find { it.id == puntoId } ?: return
        if (_uiState.value.puntoSeleccionado?.id != punto.id) {
            _uiState.update {
                it.copy(
                    puntoSeleccionado = punto,
                    valorTexto = "",
                    errores = emptyMap(),
                    aviso = null,
                    exitoso = false
                )
            }
        }
    }

    fun seleccionarPunto(punto: PuntoMedicionEntity?) {
        _uiState.update {
            it.copy(puntoSeleccionado = punto, errores = it.errores - Campos.PUNTO)
        }
    }

    /**
     * Completa un valor ficticio (como si llegara de la API simulada) para
     * agilizar la demostración del formulario.
     */
    fun simularLectura() {
        val valor = 1.2 + kotlin.random.Random.nextDouble(0.0, 5.4)
        onValorChange(String.format(java.util.Locale.US, "%.2f", valor))
    }

    fun onValorChange(texto: String) {
        // Se permite escribir mientras se valida el formato al enviar.
        _uiState.update {
            it.copy(valorTexto = texto, errores = it.errores - Campos.VALOR, aviso = null)
        }
    }

    fun onFotoCapturada(ruta: String) {
        _uiState.update { it.copy(fotoRuta = ruta) }
    }

    fun onUbicacionObtenida(texto: String) {
        _uiState.update { it.copy(ubicacionTexto = texto) }
    }

    fun limpiarAviso() {
        _uiState.update { it.copy(aviso = null) }
    }

    fun guardar() {
        val estado = _uiState.value
        val validacion = MedicionValidator.validar(
            puntoId = estado.puntoSeleccionado?.id,
            valorLibre = estado.valorTexto
        )
        if (!validacion.esValido) {
            _uiState.update { it.copy(errores = validacion.errores) }
            return
        }

        val punto = estado.puntoSeleccionado ?: return
        val valor = MedicionValidator.aNumero(estado.valorTexto)

        _uiState.update { it.copy(guardando = true, errores = emptyMap()) }
        viewModelScope.launch {
            val usuario = authRepository.usuarioSesion()
                ?: return@launch
            val medicion = medicionRepository.registrarManual(punto, valor, usuario)

            val aviso = buildString {
                append("Medición registrada: ${com.example.aquatrackx.core.ChlorineRules.formatoPpm(valor)} ppm")
                if (medicion.pendienteSincronizacion) {
                    append(" · pendiente de sincronización")
                } else if (medicion.estado != com.example.aquatrackx.domain.model.EstadoCloro.NORMAL) {
                    append(" · alerta generada automáticamente")
                }
            }

            _uiState.update {
                it.copy(
                    guardando = false,
                    exitoso = true,
                    medicionGuardada = medicion,
                    aviso = aviso
                )
            }
        }
    }

    /** Reinicia el formulario para un nuevo registro. */
    fun reiniciar() {
        _uiState.update {
            it.copy(
                valorTexto = "",
                errores = emptyMap(),
                exitoso = false,
                medicionGuardada = null,
                aviso = null,
                fotoRuta = null,
                ubicacionTexto = null
            )
        }
    }
}
