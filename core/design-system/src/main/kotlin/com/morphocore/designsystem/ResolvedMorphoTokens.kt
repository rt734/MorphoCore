package com.morphocore.designsystem

import androidx.compose.ui.graphics.Color
import com.morphocore.domain.Theme

data class ResolvedMorphoTokens(
    val disciplineAccents: Map<String, Color>,
    val difficultyColors: Map<String, Color>,
    val motionDurationShortMs: Int,
    val motionDurationMediumMs: Int,
    val motionDurationLongMs: Int,
    val activeTheme: Theme
)
