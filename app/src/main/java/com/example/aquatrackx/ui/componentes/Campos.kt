package com.example.aquatrackx.ui.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.aquatrackx.ui.theme.EstadoAdvertencia
import com.example.aquatrackx.ui.theme.EstadoCritico
import com.example.aquatrackx.ui.theme.EstadoNormal

/**
 * Campo de formulario validado con ícono, mensaje de error y contraseñas
 * con alternador de visibilidad (IE 2.1.2: validaciones visuales por campo,
 * retroalimentación clara e íconos adecuados).
 *
 * @param error mensaje de error proveniente de la validación centralizada;
 *              null = campo válido/sin tocar.
 */
@Composable
fun CampoTextoValidado(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    esPassword: Boolean = false,
    esNumerico: Boolean = false,
    icono: (@Composable () -> Unit)? = null,
    ayuda: String? = null,
    singleLine: Boolean = true,
    onFocusPerdido: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valor,
            onValueChange = onValorChange,
            label = { Text(etiqueta) },
            leadingIcon = icono,
            trailingIcon = {
                when {
                    error != null -> Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "Error en $etiqueta",
                        tint = MaterialTheme.colorScheme.error
                    )

                    esPassword -> IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar contraseña"
                            else "Mostrar contraseña"
                        )
                    }
                }
            },
            isError = error != null,
            enabled = enabled,
            singleLine = singleLine,
            visualTransformation = if (esPassword && !passwordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (esPassword) KeyboardType.Password
                else if (esNumerico) KeyboardType.Decimal else KeyboardType.Text
            ),
            supportingText = {
                val mensaje = error ?: ayuda
                if (mensaje != null) {
                    Text(
                        text = mensaje,
                        color = if (error != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (error != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                focusedLabelColor = if (error != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = etiqueta }
        )
    }
}

/** Chip que muestra el estado del cloro con color animado (retroalimentación visual). */
@Composable
fun ChipEstado(
    estado: String,
    modifier: Modifier = Modifier
) {
    val color = colorDeEstado(estado)
    val colorAnimado by animateColorAsState(
        targetValue = color,
        animationSpec = tween(400),
        label = "chipEstado"
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = colorAnimado)
        }
        Text(
            text = estado.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelMedium,
            color = colorAnimado,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
    }
}

/** Color asociado a un estado de cloro (normal / advertencia / crítico). */
fun colorDeEstado(estado: String): Color = when (estado) {
    "NORMAL" -> EstadoNormal
    "ADVERTENCIA" -> EstadoAdvertencia
    "CRITICO" -> EstadoCritico
    else -> Color(0xFF546E7A)
}

/** Barra de progreso animada simple para resúmenes. */
@Composable
fun BarraProgresoAnimada(
    progreso: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animado by animateFloatAsState(
        targetValue = progreso.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "barra"
    )
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        drawRoundRect(
            color = color.copy(alpha = 0.2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        drawRoundRect(
            color = color,
            size = androidx.compose.ui.geometry.Size(
                width = size.width * animado,
                height = size.height
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
    }
}
