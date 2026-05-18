package com.morphocore.domain

data class MorphoTypography(
    val displayFontPath: String?,
    val bodyFontPath: String?,
    val labelFontPath: String?,
    val scale: Map<String, TextScaleEntry>
)

data class TextScaleEntry(val sizeSp: Int, val weight: Int, val lineHeightSp: Int)
