package com.morphocore.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.morphocore.domain.MorphoTypography
import com.morphocore.domain.TextScaleEntry

internal fun MorphoTypography.toMaterial3Typography(): Typography {
    fun entry(key: String, default: TextStyle): TextStyle =
        scale[key]?.toTextStyle() ?: default
    val base = Typography()
    return Typography(
        displayLarge  = entry("displayLarge",  base.displayLarge),
        headlineLarge = entry("headlineLarge", base.headlineLarge),
        titleLarge    = entry("titleLarge",    base.titleLarge),
        bodyLarge     = entry("bodyLarge",     base.bodyLarge),
        labelLarge    = entry("labelLarge",    base.labelLarge)
    )
}

private fun TextScaleEntry.toTextStyle() = TextStyle(
    fontSize   = sizeSp.sp,
    fontWeight = FontWeight(weight),
    lineHeight = lineHeightSp.sp
)
