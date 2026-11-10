package com.fasdai.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ButtonShapeStyle { SHARP, ROUNDED, PILL }

private val background = Color(0xFF0E0E12)
private val surface = Color(0xFF17171D)
private val surfaceVariant = Color(0xFF1F1F27)
private val onSurface = Color(0xFFEDEDF2)
private val onSurfaceMuted = Color(0xFF9A9AA6)
private val danger = Color(0xFFFF4D4D)

fun accentColor(hex: Int): Color = Color(hex or 0xFF000000.toInt())

fun shapesFor(style: ButtonShapeStyle): Shapes {
    val corner = when (style) {
        ButtonShapeStyle.SHARP -> 2.dp
        ButtonShapeStyle.ROUNDED -> 14.dp
        ButtonShapeStyle.PILL -> 28.dp
    }
    return Shapes(
        extraSmall = RoundedCornerShape(corner / 2),
        small = RoundedCornerShape(corner),
        medium = RoundedCornerShape(corner),
        large = RoundedCornerShape(corner),
        extraLarge = RoundedCornerShape(corner),
    )
}

@Composable
fun FasdAiTheme(
    accent: Color,
    shapeStyle: ButtonShapeStyle,
    content: @Composable () -> Unit,
) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.Black,
        secondary = accent,
        onSecondary = Color.Black,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceMuted,
        error = danger,
        onError = Color.Black,
    )
    MaterialTheme(
        colorScheme = scheme,
        shapes = shapesFor(shapeStyle),
        typography = FasdAiTypography,
        content = content,
    )
}
