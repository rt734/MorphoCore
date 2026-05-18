package com.morphocore.feature.movements.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morphocore.domain.Difficulty
import com.morphocore.domain.Movement
import com.morphocore.domain.MuscleGroup

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovementRow(movement: Movement, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = movement.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            DifficultyBadge(movement.difficulty)
        }
        if (movement.muscles.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                movement.muscles.take(3).forEach { muscle ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = muscleLabel(muscle),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    val (label, color) = when (difficulty) {
        Difficulty.BEGINNER     -> "Beginner"     to MaterialTheme.colorScheme.secondary
        Difficulty.INTERMEDIATE -> "Intermediate" to MaterialTheme.colorScheme.tertiary
        Difficulty.ADVANCED     -> "Advanced"     to MaterialTheme.colorScheme.error
    }
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
}

private fun muscleLabel(muscle: MuscleGroup): String = when (muscle) {
    MuscleGroup.Quadriceps  -> "Quads"
    MuscleGroup.Hamstrings  -> "Hamstrings"
    MuscleGroup.Glutes      -> "Glutes"
    MuscleGroup.Core        -> "Core"
    MuscleGroup.Shoulders   -> "Shoulders"
    MuscleGroup.Back        -> "Back"
    MuscleGroup.Chest       -> "Chest"
    MuscleGroup.Calves      -> "Calves"
    MuscleGroup.HipFlexors  -> "Hip Flexors"
    is MuscleGroup.Unknown  -> muscle.raw
}
