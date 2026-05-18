package com.morphocore.designsystem

import androidx.compose.runtime.compositionLocalOf

val LocalMorphoTheme = compositionLocalOf<ResolvedMorphoTokens> {
    error("No MorphoTheme provided — wrap your UI in MorphoTheme { }")
}
