package com.morphocore.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.morphocore.domain.MorphoColors

internal fun MorphoColors.toMaterial3ColorScheme(): ColorScheme {
    val base = if (isLightBackground(background)) lightColorScheme() else darkColorScheme()
    return base.copy(
        primary          = Color(primary),
        onPrimary        = Color(onPrimary),
        secondary        = Color(secondary),
        onSecondary      = Color(onSecondary),
        background       = Color(background),
        onBackground     = Color(onBackground),
        surface          = Color(surface),
        onSurface        = Color(onSurface),
        surfaceVariant   = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        outline          = Color(outline)
    )
}

private fun isLightBackground(argb: Long): Boolean {
    val r = ((argb shr 16) and 0xFF).toFloat() / 255f
    val g = ((argb shr 8) and 0xFF).toFloat() / 255f
    val b = (argb and 0xFF).toFloat() / 255f
    return (0.2126f * r + 0.7152f * g + 0.0722f * b) > 0.5f
}
