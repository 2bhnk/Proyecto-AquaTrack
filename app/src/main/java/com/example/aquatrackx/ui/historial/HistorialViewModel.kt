package com.example.aquatrackx.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquatrackx.data.local.GranjaEntity
import com.example.aquatrackx.data.local.MedicionEntity
import com.example.aquatrackx.data.local.PuntoMedicionEntity
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
 * Historial de mediciones con filtros por período (24 h / 7 días / 30 días,
 * pregunta 27), granja y punto de medición, más totales por estado.
 */
data class HistorialUiState(
    val cargando: Boolean = true,
    val periodo: PeriodoHistorial = PeriodoHistorial.H24,
    val granjas: List<GranjaEntity> = emptyList(),
    val puntos: List<PuntoMedicionEntity> = emptyList(),
    val granjaFiltrada: GranjaEntity? = null,
    val puntoFiltrado: PuntoMedicionEntity? = null,
    val mediciones: List<MedicionEntity> = emptyList(),
    val totalNormal: Int = 0,
    val totalAdvertencia: Int = 0,
    val totalCritico: Int = 0
)

class HistorialViewModel(
    private val authRepository: AuthRepository,
    private val catalogoRepository: CatalogoRepository,
    private val medicionRepository: MedicionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    private var todasLasMediciones: List<MedicionEntity> = emptyList()

    init {
        viewModelScope.launch {
            val usuario = authRepository.usuarioSesion() ?: return@launch

            catalogoRepository.granjasVisibles(usuario).collect { granjas ->
                _uiState.update { it.copy(granjas = granjas) }
            }

            catalogoRepository.puntosVisibles(usuario).collect { puntos ->
                _uiState.update { it.copy(puntos = puntos) }

                val ids = puntos.map { it.id }
                if (ids.isNotEmpty()) {
                    val desde = System.currentTimeMillis() -
                        PeriodoHistorial.D30.horas * 60 * 60 * 1000L
                    medicionRepository.medicionesDesde(desde, ids).collect { mediciones ->
                        todasLasMediciones = mediciones
                        aplicarFiltros()
                        _uiState.update { it.copy(cargando = false) }
                    }
                }
            }
        }
    }

    fun cambiarPeriodo(periodo: PeriodoHistorial) {
        _uiState.update { it.copy(periodo = periodo) }
        aplicarFiltros()
    }

    fun filtrarPorGranja(granja: GranjaEntity?) {
        _uiState.update {
            it.copy(
                granjaFiltrada = granja,
                // Al cambiar de granja, el punto filtrado deja de ser válido.
                puntoFiltrado = if (granja == null) it.puntoFiltrado
                else it.puntoFiltrado?.takeIf { p -> p.granjaId == granja.id }
            )
        }
        aplicarFiltros()
    }

    fun filtrarPorPunto(punto: PuntoMedicionEntity?) {
        _uiState.update { it.copy(puntoFiltrado = punto) }
        aplicarFiltros()
    }

    fun limpiarFiltros() {
        _uiState.update { it.copy(granjaFiltrada = null, puntoFiltrado = null) }
        aplicarFiltros()
    }

    private fun aplicarFiltros() {
        val estado = _uiState.value
        val limite = System.currentTimeMillis() - estado.periodo.horas * 60 * 60 * 1000L

        val granjaId = estado.granjaFiltrada?.id
        val puntoId = estado.puntoFiltrado?.id
        val puntosPorGranja = estado.puntos
            .filter { granjaId == null || it.granjaId == granjaId }
            .map { it.id }
            .toSet()

        val filtradas = todasLasMediciones.filter { medicion ->
            medicion.fecha >= limite &&
                medicion.puntoId in puntosPorGranja &&
                (puntoId == null || medicion.puntoId == puntoId)
        }.sortedByDescending { it.fecha }

        _uiState.update {
            it.copy(
                mediciones = filtradas,
                totalNormal = filtradas.count { m -> m.estado.name == "NORMAL" },
                totalAdvertencia = filtradas.count { m -> m.estado.name == "ADVERTENCIA" },
                totalCritico = filtradas.count { m -> m.estado.name == "CRITICO" }
            )
        }
    }
}
