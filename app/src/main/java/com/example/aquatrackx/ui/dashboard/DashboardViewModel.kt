package com.example.aquatrackx.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquatrackx.core.ChlorineRules
import com.example.aquatrackx.data.local.AlertaEntity
import com.example.aquatrackx.data.local.GranjaEntity
import com.example.aquatrackx.data.local.MedicionEntity
import com.example.aquatrackx.data.local.PuntoMedicionEntity
import com.example.aquatrackx.data.local.UsuarioEntity
import com.example.aquatrackx.data.repository.AlertaRepository
import com.example.aquatrackx.data.repository.AuthRepository
import com.example.aquatrackx.data.repository.CatalogoRepository
import com.example.aquatrackx.data.repository.MedicionRepository
import com.example.aquatrackx.domain.model.EstadoAlerta
import com.example.aquatrackx.domain.model.EstadoCloro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Panel principal (pregunta 28): mediciones actuales, alertas activas,
 * promedio / máximo / mínimo, comparación entre puntos y gráfico histórico.
 */
data class DashboardUiState(
    val cargando: Boolean = true,
    val usuario: UsuarioEntity? = null,
    val granjas: List<GranjaEntity> = emptyList(),
    /** Última medición por punto, con su punto asociado. */
    val estadoPuntos: List<EstadoPunto> = emptyList(),
    val alertasAbiertas: List<AlertaEntity> = emptyList(),
    val criticasAbiertas: Int = 0,
    val pendientesSincronizacion: Int = 0,
    val promedio24h: Double? = null,
    val maximo24h: Double? = null,
    val minimo24h: Double? = null,
    /** Serie para el gráfico de líneas del panel (promedio por hora, 24 h). */
    val serieHistorica: List<PuntoSerie> = emptyList(),
    val ultimaSincronizacion: Long? = null,
    val sincronizando: Boolean = false,
    val modoSinConexion: Boolean = false
)

data class EstadoPunto(
    val punto: PuntoMedicionEntity,
    val granja: GranjaEntity?,
    val ultima: MedicionEntity?
)

data class PuntoSerie(val x: Float, val y: Float)

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val catalogoRepository: CatalogoRepository,
    private val medicionRepository: MedicionRepository,
    private val alertaRepository: AlertaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            val usuario = authRepository.usuarioSesion() ?: return@launch
            _uiState.update {
                it.copy(usuario = usuario, modoSinConexion = authRepository.modoSinConexion)
            }

            // Suscripciones reactivas: la UI se actualiza sola ante cambios de estado.
            catalogoRepository.granjasVisibles(usuario).collect { granjas ->
                _uiState.update { it.copy(granjas = granjas, cargando = false) }
            }
        }

        viewModelScope.launch {
            val usuario = authRepository.usuarioSesion() ?: return@launch
            catalogoRepository.puntosVisibles(usuario).collect { puntos ->
                val granjas = _uiState.value.granjas
                medicionRepository.ultimasDeTodosLosPuntos().collect { ultimas ->
                    val porPunto = ultimas.groupBy { it.puntoId }
                    val estados = puntos.map { punto ->
                        EstadoPunto(
                            punto = punto,
                            granja = granjas.firstOrNull { it.id == punto.granjaId },
                            ultima = porPunto[punto.id]?.maxByOrNull { it.fecha }
                        )
                    }
                    _uiState.update { it.copy(estadoPuntos = estados) }
                    recalcularAgregados(puntos.map { p -> p.id })
                }
            }
        }

        viewModelScope.launch {
            alertaRepository.porEstado(EstadoAlerta.ABIERTA).collect { alertas ->
                val usuario = _uiState.value.usuario ?: return@collect
                val visibles = if (usuario.granjaIds.isEmpty()) alertas
                else catalogoRepositoryFiltrar(alertas, usuario)
                _uiState.update {
                    it.copy(
                        alertasAbiertas = visibles,
                        criticasAbiertas = visibles.count { a -> a.severidad == EstadoCloro.CRITICO }
                    )
                }
            }
        }

        viewModelScope.launch {
            medicionRepository.pendientesDeSincronizacion().collect { pendientes ->
                _uiState.update { it.copy(pendientesSincronizacion = pendientes) }
            }
        }

        sincronizar()
    }

    private suspend fun catalogoRepositoryFiltrar(
        alertas: List<AlertaEntity>,
        usuario: UsuarioEntity
    ): List<AlertaEntity> {
        val puntos = _uiState.value.estadoPuntos.map { it.punto.id }.toSet()
        return alertas.filter { it.puntoId in puntos }
    }

    private suspend fun recalcularAgregados(puntoIds: List<Long>) {
        if (puntoIds.isEmpty()) return
        val desde = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val dao = medicionRepository
        val promedio = promedioDesde(desde, puntoIds)
        val maximo = maximoDesde(desde, puntoIds)
        val minimo = minimoDesde(desde, puntoIds)
        val serie = serie24h(desde, puntoIds)
        _uiState.update {
            it.copy(promedio24h = promedio, maximo24h = maximo, minimo24h = minimo, serieHistorica = serie)
        }
    }

    // Helpers que delegan en el repositorio (expuestos como métodos para testear).
    private suspend fun promedioDesde(desde: Long, ids: List<Long>): Double? =
        medicionRepository.promedioDesde(desde, ids)

    private suspend fun maximoDesde(desde: Long, ids: List<Long>): Double? =
        medicionRepository.maximoDesde(desde, ids)

    private suspend fun minimoDesde(desde: Long, ids: List<Long>): Double? =
        medicionRepository.minimoDesde(desde, ids)

    private suspend fun serie24h(desde: Long, ids: List<Long>): List<PuntoSerie> {
        val mediciones = medicionRepository.listarDesde(desde, ids)
        if (mediciones.isEmpty()) return emptyList()
        // Promedio por hora para el gráfico del panel.
        val porHora = mediciones.groupBy { it.fecha / (60 * 60 * 1000L) }
        val horas = porHora.keys.sorted()
        if (horas.isEmpty()) return emptyList()
        val primera = horas.first()
        return horas.map { hora ->
            val valores = porHora.getValue(hora).map { it.valor }
            PuntoSerie(
                x = (hora - primera).toFloat(),
                y = valores.average().toFloat()
            )
        }
    }

    /** Descarga nuevas mediciones de la API simulada y notifica críticas. */
    fun sincronizar() {
        if (_uiState.value.sincronizando) return
        _uiState.update { it.copy(sincronizando = true) }
        viewModelScope.launch {
            val criticas = medicionRepository.sincronizar()
            _uiState.update {
                it.copy(
                    sincronizando = false,
                    ultimaSincronizacion = System.currentTimeMillis(),
                    modoSinConexion = authRepository.modoSinConexion
                )
            }
            onAlertasCriticas?.invoke(criticas)
        }
    }

    /** Callback externo (Activity) para disparar notificaciones nativas. */
    var onAlertasCriticas: ((List<AlertaEntity>) -> Unit)? = null
}
