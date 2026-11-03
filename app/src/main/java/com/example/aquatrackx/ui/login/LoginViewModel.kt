package com.example.aquatrackx.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aquatrackx.core.validacion.Campos
import com.example.aquatrackx.core.validacion.LoginValidator
import com.example.aquatrackx.core.validacion.ResultadoValidacion
import com.example.aquatrackx.data.repository.AuthRepository
import com.example.aquatrackx.data.repository.ResultadoLogin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de inicio de sesión.
 * Los errores por campo llegan desde [LoginValidator] (lógica desacoplada, IE 2.2.1)
 * y la UI solo los renderiza con íconos y mensajes (IE 2.1.2).
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val errores: Map<String, String> = emptyMap(),
    val errorGeneral: String? = null,
    val cargando: Boolean = false,
    val exitoso: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(valor: String) {
        _uiState.update {
            it.copy(
                email = valor,
                errores = it.errores - Campos.EMAIL,
                errorGeneral = null
            )
        }
    }

    fun onPasswordChange(valor: String) {
        _uiState.update {
            it.copy(
                password = valor,
                errores = it.errores - Campos.PASSWORD,
                errorGeneral = null
            )
        }
    }

    /** Validación en tiempo real al salir del campo (retroalimentación inmediata). */
    fun validarCampo(campo: String) {
        val estado = _uiState.value
        val resultado: ResultadoValidacion = when (campo) {
            Campos.EMAIL -> LoginValidator.validar(estado.email, "").let {
                ResultadoValidacion(
                    if (it[Campos.EMAIL] != null) mapOf(Campos.EMAIL to it[Campos.EMAIL]!!)
                    else emptyMap()
                )
            }
            Campos.PASSWORD -> LoginValidator.validar("", estado.password).let {
                ResultadoValidacion(
                    if (it[Campos.PASSWORD] != null) mapOf(Campos.PASSWORD to it[Campos.PASSWORD]!!)
                    else emptyMap()
                )
            }
            else -> ResultadoValidacion.Vacio
        }
        _uiState.update { estado ->
            val errores = if (resultado.esValido) estado.errores - campo
            else estado.errores + resultado.errores
            estado.copy(errores = errores)
        }
    }

    fun iniciarSesion() {
        val estado = _uiState.value
        val validacion = LoginValidator.validar(estado.email, estado.password)
        if (!validacion.esValido) {
            _uiState.update { it.copy(errores = validacion.errores, errorGeneral = null) }
            return
        }

        _uiState.update { it.copy(cargando = true, errores = emptyMap(), errorGeneral = null) }
        viewModelScope.launch {
            when (val resultado = authRepository.login(estado.email, estado.password)) {
                is ResultadoLogin.Exitoso ->
                    _uiState.update { it.copy(cargando = false, exitoso = true) }

                is ResultadoLogin.Error ->
                    _uiState.update {
                        it.copy(cargando = false, errorGeneral = resultado.mensaje)
                    }
            }
        }
    }

    /** Acceso rápido para la demo (créditos de prueba en la pantalla). */
    fun completarDemo(email: String) {
        _uiState.update {
            it.copy(email = email, password = "123456", errores = emptyMap(), errorGeneral = null)
        }
    }
}
