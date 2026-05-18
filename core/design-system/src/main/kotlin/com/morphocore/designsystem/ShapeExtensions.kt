package com.morphocore.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import com.morphocore.domain.MorphoShapes

internal fun MorphoShapes.toMaterial3Shapes() = Shapes(
    small  = RoundedCornerShape(smallDp.dp),
    medium = RoundedCornerShape(mediumDp.dp),
    large  = RoundedCornerShape(largeDp.dp)
)
