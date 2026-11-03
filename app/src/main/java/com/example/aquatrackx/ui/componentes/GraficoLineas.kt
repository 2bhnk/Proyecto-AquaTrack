package com.example.aquatrackx.ui.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Gráfico de líneas simple dibujado con Canvas (sin dependencias externas).
 * La línea se dibuja con una animación de progreso al componer o cambiar los
 * datos (IE 2.2.2: animaciones que aportan fluidez a la visualización).
 *
 * @param datos serie de valores (ej. ppm en el tiempo)
 * @param colorLínea color principal; si es null se usa el color primario por parámetro.
 */
@Composable
fun GraficoLineas(
    datos: List<Float>,
    modifier: Modifier = Modifier,
    colorLinea: Color,
    altura: Dp = 160.dp
) {
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(datos) {
        progreso.snapTo(0f)
        progreso.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(altura)
    ) {
        if (datos.size < 2) return@Canvas

        val ancho = size.width
        val alto = size.height
        val min = datos.min()
        val max = datos.max()
        val rango = (max - min).takeIf { it > 0f } ?: 1f
        val pasoX = ancho / (datos.size - 1)

        fun punto(indice: Int): Offset {
            val x = pasoX * indice
            val normalizado = (datos[indice] - min) / rango
            // Margen vertical del 10% para que la línea no toque los bordes.
            val y = alto * 0.9f - (alto * 0.8f) * normalizado
            return Offset(x, y)
        }

        val path = Path()
        val area = Path()
        val limite = ((datos.size - 1) * progreso.value).toInt()
            .coerceIn(0, datos.size - 1)

        val primero = punto(0)
        path.moveTo(primero.x, primero.y)
        area.moveTo(primero.x, alto)
        area.lineTo(primero.x, primero.y)

        for (i in 1..limite) {
            val p = punto(i)
            path.lineTo(p.x, p.y)
            area.lineTo(p.x, p.y)
        }
        val ultimo = punto(limite)
        area.lineTo(ultimo.x, alto)
        area.close()

        // Relleno degradado bajo la línea.
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                colors = listOf(
                    colorLinea.copy(alpha = 0.25f),
                    colorLinea.copy(alpha = 0.02f)
                )
            )
        )
        // Línea principal.
        drawPath(
            path = path,
            color = colorLinea,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        // Punto final destacado.
        drawCircle(color = colorLinea, radius = 5.dp.toPx(), center = ultimo)
        drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = ultimo)
    }
}
