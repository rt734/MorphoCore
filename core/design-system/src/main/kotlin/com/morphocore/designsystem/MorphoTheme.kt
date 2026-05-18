package com.morphocore.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.morphocore.domain.Theme

@Composable
fun MorphoTheme(theme: Theme, content: @Composable () -> Unit) {
    val colorScheme = theme.colors.toMaterial3ColorScheme()
    val typography  = theme.typography.toMaterial3Typography()
    val shapes      = theme.shapes.toMaterial3Shapes()
    val tokens      = theme.toResolvedMorphoTokens(fallbackColor = colorScheme.primary)

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = typography,
        shapes      = shapes
    ) {
        CompositionLocalProvider(LocalMorphoTheme provides tokens) {
            content()
        }
    }
}

private fun Theme.toResolvedMorphoTokens(fallbackColor: Color): ResolvedMorphoTokens {
    val resolved = colors.semantic.mapValues { (_, argb) -> Color(argb) }
    return ResolvedMorphoTokens(
        disciplineAccents = listOf("karate", "kungfu", "yoga", "gym", "calisthenics")
            .associateWith { id -> resolved["discipline.$id"] ?: fallbackColor },
        difficultyColors = mapOf(
            "easy"   to (resolved["difficulty.easy"]   ?: Color(0xFF4CAF50L)),
            "medium" to (resolved["difficulty.medium"] ?: Color(0xFFFF9800L)),
            "hard"   to (resolved["difficulty.hard"]   ?: Color(0xFFF44336L))
        ),
        motionDurationShortMs  = motion.durationShortMs,
        motionDurationMediumMs = motion.durationMediumMs,
        motionDurationLongMs   = motion.durationLongMs,
        activeTheme = this
    )
}
