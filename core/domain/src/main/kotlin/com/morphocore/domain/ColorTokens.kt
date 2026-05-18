package com.morphocore.domain

// Colors are stored as ARGB Long values (0xFFRRGGBB).
// The design-system layer converts these to androidx.compose.ui.graphics.Color.
data class ColorTokens(
    val primary: Long,
    val onPrimary: Long,
    val surface: Long,
    val onSurface: Long,
    val background: Long,
    val onBackground: Long,
    val accent: Long
)
