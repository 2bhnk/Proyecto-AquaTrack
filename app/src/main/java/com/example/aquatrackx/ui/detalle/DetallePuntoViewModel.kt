package com.example.aquatrackx.ui.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquatrackx.core.ChlorineRules
import com.example.aquatrackx.data.local.AlertaEntity
import com.example.aquatrackx.data.local.MedicionEntity
import com.example.aquatrackx.data.local.PuntoMedicionEntity
import com.example.aquatrackx.data.repository.AlertaRepository
import com.example.aquatrackx.data.repository.AuthRepository
import com.example.aquatrackx.data.repository.CatalogoRepository
import com.example.aquatrackx.data.repository.MedicionRepository
import com.example.aquatrackx.domain.model.PeriodoHistorial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Detalle del punto de medición: medidor animado, estado, resumen del período,
 * gráfico histórico con filtro 24 h / 7 días / 30 días (pregunta 27) y alerta abierta.
 */
data class DetallePuntoUiState(
    val cargando: Boolean = true,
    val punto: PuntoMedicionEntity? = null,
    val ultima: MedicionEntity? = null,
    val periodo: PeriodoHistorial = PeriodoHistorial.H24,
    val historial: List<MedicionEntity> = emptyList(),
    val promedio: Double? = null,
    val maximo: Double? = null,
    val minimo: Double? = null,
    val alertaAbierta: AlertaEntity? = null,
    /** Serie normalizada para el gráfico (x: índice, y: valor). */
    val serie: List<Float> = emptyList()
)

class DetallePuntoViewModel(
    private val authRepository: AuthRepository,
    private val catalogoRepository: CatalogoRepository,
    private val medicionRepository: MedicionRepository,
    private val alertaRepository: AlertaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetallePuntoUiState())
    val uiState: StateFlow<DetallePuntoUiState> = _uiState.asStateFlow()

    private var puntoId: Long = -1L

    fun cargar(id: Long) {
        if (id == puntoId && !_uiState.value.cargando) return
        puntoId = id

        viewModelScope.launch {
            catalogoRepository.puntoFlow(id).collect { punto ->
                _uiState.update { it.copy(punto = punto, cargando = punto == null && it.cargando) }
                punto?.let { p -> observarMediciones(p) }
            }
        }

        viewModelScope.launch {
            medicionRepository.ultimaDePunto(id).collect { ultima ->
                _uiState.update { it.copy(ultima = ultima) }
            }
        }

        viewModelScope.launch {
            alertaRepository.todas().collect { alertas ->
                _uiState.update {
                    it.copy(alertaAbierta = alertas.firstOrNull {
                        a -> a.puntoId == id &&
                        a.estado == com.example.aquatrackx.domain.model.EstadoAlerta.ABIERTA
                    })
                }
            }
        }
    }

    private fun observarMediciones(punto: PuntoMedicionEntity) {
        viewModelScope.launch {
            medicionRepository.medicionesDePunto(punto.id).collect { todas ->
                aplicarPeriodo(todas, _uiState.value.periodo)
            }
        }
    }

    fun cambiarPeriodo(periodo: PeriodoHistorial) {
        _uiState.update { it.copy(periodo = periodo) }
        val punto = _uiState.value.punto ?: return
        viewModelScope.launch {
            medicionRepository.medicionesDePunto(punto.id).collect { todas ->
                aplicarPeriodo(todas, periodo)
            }
        }
    }

    private fun aplicarPeriodo(todas: List<MedicionEntity>, periodo: PeriodoHistorial) {
        val limite = System.currentTimeMillis() - periodo.horas * 60 * 60 * 1000L
        val filtradas = todas.filter { it.fecha >= limite }.sortedByDescending { it.fecha }
        val valores = filtradas.map { it.valor }
        // Muestreo para el gráfico: máximo 60 puntos dibujados.
        val paso = if (filtradas.size > 60) filtradas.size / 60 else 1
        val serie = filtradas
            .sortedBy { it.fecha }
            .filterIndexed { indice, _ -> indice % paso == 0 }
            .map { it.valor.toFloat() }

        _uiState.update {
            it.copy(
                periodo = periodo,
                historial = filtradas,
                promedio = valores.takeIf { v -> v.isNotEmpty() }?.average(),
                maximo = valores.maxOrNull(),
                minimo = valores.minOrNull(),
                serie = serie
            )
        }
    }

    /** Referencia al rango configurado del punto (para mensajes de la UI). */
    fun rangoNormal(): String {
        val punto = _uiState.value.punto ?: return ""
        return "${ChlorineRules.formatoPpm(punto.normalMin)} – ${ChlorineRules.formatoPpm(punto.normalMax)} ppm"
    }
}
