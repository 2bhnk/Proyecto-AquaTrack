package com.example.aquatrackx.ui.puntos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquatrackx.data.local.GranjaEntity
import com.example.aquatrackx.data.local.MedicionEntity
import com.example.aquatrackx.data.local.PuntoMedicionEntity
import com.example.aquatrackx.data.local.UsuarioEntity
import com.example.aquatrackx.data.repository.AuthRepository
import com.example.aquatrackx.data.repository.CatalogoRepository
import com.example.aquatrackx.data.repository.MedicionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Lista jerárquica granja -> puntos de medición con su estado actual
 * (consulta desde terreno, mejoras esperadas del caso).
 */
data class PuntosUiState(
    val cargando: Boolean = true,
    val agrupado: List<GrupoGranja> = emptyList()
)

data class GrupoGranja(
    val granja: GranjaEntity,
    val puntos: List<ItemPunto>
)

data class ItemPunto(
    val punto: PuntoMedicionEntity,
    val ultima: MedicionEntity?
)

class PuntosViewModel(
    private val authRepository: AuthRepository,
    private val catalogoRepository: CatalogoRepository,
    private val medicionRepository: MedicionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PuntosUiState())
    val uiState: StateFlow<PuntosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val usuario: UsuarioEntity = authRepository.usuarioSesion() ?: return@launch

            catalogoRepository.granjasVisibles(usuario).collect { granjas ->
                catalogoRepository.puntosVisibles(usuario).collect { puntos ->
                    medicionRepository.ultimasDeTodosLosPuntos().collect { ultimas ->
                        val porPunto = ultimas.groupBy { it.puntoId }
                        val grupos = granjas.map { granja ->
                            GrupoGranja(
                                granja = granja,
                                puntos = puntos
                                    .filter { it.granjaId == granja.id }
                                    .map { punto ->
                                        ItemPunto(
                                            punto = punto,
                                            ultima = porPunto[punto.id]?.maxByOrNull { it.fecha }
                                        )
                                    }
                            )
                        }.filter { it.puntos.isNotEmpty() }
                        _uiState.update { it.copy(agrupado = grupos, cargando = false) }
                    }
                }
            }
        }
    }
}
