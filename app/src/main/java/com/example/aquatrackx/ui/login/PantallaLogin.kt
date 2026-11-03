package com.example.aquatrackx.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.aquatrackx.R
import com.example.aquatrackx.core.validacion.Campos
import com.example.aquatrackx.ui.componentes.CampoTextoValidado
import com.example.aquatrackx.ui.theme.AquaPrimary

/**
 * Pantalla de inicio de sesión con formulario validado por campo
 * (íconos, mensajes de error y validación desacoplada — IE 2.1.2).
 */
@Composable
fun PantallaLogin(
    viewModel: LoginViewModel,
    onLoginExitoso: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current

    // Navega al panel cuando la autenticación es correcta.
    LaunchedEffect(estado.exitoso) {
        if (estado.exitoso) onLoginExitoso()
    }

    // Aparición escalonada de los elementos (animación de entrada).
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Identidad visual de la app.
                        Surface(
                            modifier = Modifier.size(96.dp),
                            shape = CircleShape,
                            color = AquaPrimary.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.WaterDrop,
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp),
                                    tint = AquaPrimary
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "AquaTrackX",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Medición de cloro en línea",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Control de calidad del agua · sector avícola y ganadero",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(28.dp))
                    }
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 2 }
                ) {
                    Column {
                        // Error general (credenciales incorrectas).
                        estado.errorGeneral?.let { error ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = error,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        CampoTextoValidado(
                            valor = estado.email,
                            onValorChange = viewModel::onEmailChange,
                            etiqueta = "Correo electrónico",
                            error = estado.errores[Campos.EMAIL],
                            ayuda = "Ej: operario@duoc.cl",
                            icono = {
                                Icon(Icons.Filled.Email, contentDescription = null)
                            },
                            onFocusPerdido = { viewModel.validarCampo(Campos.EMAIL) }
                        )

                        Spacer(Modifier.height(8.dp))

                        CampoTextoValidado(
                            valor = estado.password,
                            onValorChange = viewModel::onPasswordChange,
                            etiqueta = "Contraseña",
                            error = estado.errores[Campos.PASSWORD],
                            esPassword = true,
                            icono = {
                                Icon(Icons.Filled.Lock, contentDescription = null)
                            },
                            onFocusPerdido = { viewModel.validarCampo(Campos.PASSWORD) }
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = viewModel::iniciarSesion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !estado.cargando
                        ) {
                            if (estado.cargando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Person, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Ingresar", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Usuarios de demostración (contraseña 123456):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        FilaDemo("Operario", "operario@duoc.cl", viewModel)
                        FilaDemo("Supervisor", "supervisor@duoc.cl", viewModel)
                        FilaDemo("Jefatura", "jefatura@duoc.cl", viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaDemo(rol: String, email: String, viewModel: LoginViewModel) {
    TextButton(onClick = { viewModel.completarDemo(email) }) {
        Icon(
            imageVector = Icons.Filled.Password,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = "$rol · $email",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
