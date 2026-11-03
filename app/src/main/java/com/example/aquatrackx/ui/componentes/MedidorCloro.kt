package com.example.aquatrackx.ui.componentes

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquatrackx.core.ChlorineRules
import com.example.aquatrackx.ui.theme.EstadoAdvertencia
import com.example.aquatrackx.ui.theme.EstadoCritico
import com.example.aquatrackx.ui.theme.EstadoNormal

/**
 * Medidor circular animado del nivel de cloro (IE 2.2.2: animaciones que
 * aportan retroalimentación). El arco y el valor se animan al cambiar,
 * y en estado crítico el anillo pulsa para destacar la gravedad.
 */
@Composable
fun MedidorCloro(
    valor: Double,
    estado: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 180.dp
) {
    val color = when (estado) {
        "NORMAL" -> EstadoNormal
        "ADVERTENCIA" -> EstadoAdvertencia
        "CRITICO" -> EstadoCritico
        else -> MaterialTheme.colorScheme.primary
    }

    val objetivo = ChlorineRules.normalizarParaGauge(valor)
    val progreso by animateFloatAsState(
        targetValue = objetivo,
        animationSpec = tween(durationMillis = 900),
        label = "gauge"
    )

    // Pulso en estado crítico para destacar la alerta.
    val transicion = rememberInfiniteTransition(label = "pulso")
    val pulso by transicion.animateFloat(
        initialValue = 1f,
        targetValue = if (estado == "CRITICO") 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsoValor"
    )

    Box(modifier = modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(tamano)) {
            val grosor = 14.dp.toPx()
            val inset = grosor / 2
            val arcSize = Size(size.width - grosor, size.height - grosor)

            // Fondo del arco.
            drawArc(
                color = color.copy(alpha = 0.15f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = grosor, cap = StrokeCap.Round)
            )
            // Progreso.
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * progreso * pulso,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = grosor, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = ChlorineRules.formatoPpm(valor),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "ppm",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = estado.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
