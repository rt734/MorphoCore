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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.morphocore.domain.AnimationClip
import com.morphocore.domain.Difficulty
import com.morphocore.domain.Movement
import com.morphocore.domain.MuscleGroup

private fun findHighlightRange(text: String, query: String): IntRange? {
    val q = query.trim().lowercase()
    if (q.isBlank()) return null
    val idx = text.lowercase().indexOf(q)
    if (idx == -1) return null
    return idx until idx + q.length
}

internal fun descriptionMatchSnippet(description: String, query: String): String? {
    val q = query.trim().lowercase()
    if (q.isBlank()) return null
    val idx = description.lowercase().indexOf(q)
    if (idx == -1) return null
    val start = maxOf(0, idx - 25)
    val end = minOf(description.length, idx + q.length + 25)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < description.length) "…" else ""
    return "$prefix${description.substring(start, end)}$suffix"
}

@Composable
private fun highlightedAnnotatedString(text: String, query: String) =
    findHighlightRange(text, query)?.let { range ->
        buildAnnotatedString {
            append(text.substring(0, range.first))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(range)) }
            append(text.substring(range.last + 1))
        }
    } ?: buildAnnotatedString { append(text) }

@Composable
private fun highlightedName(name: String, query: String) =
    findHighlightRange(name, query)?.let { range ->
        buildAnnotatedString {
            append(name.substring(0, range.first))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(name.substring(range)) }
            append(name.substring(range.last + 1))
        }
    } ?: buildAnnotatedString { append(name) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovementRow(
    movement: Movement,
    onClick: () -> Unit,
    onTagClick: ((String) -> Unit)? = null,
    query: String = "",
    modifier: Modifier = Modifier
) {
    val nameMatches = query.isNotBlank() && findHighlightRange(movement.name, query) != null
    val snippet = if (!nameMatches) descriptionMatchSnippet(movement.description, query) else null

    Column(
        modifier = modifier
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
                text = highlightedName(movement.name, query),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            DifficultyBadge(movement.difficulty)
        }
        val clipMeta = clipMetaLabel(movement.clips)
        if (clipMeta != null) {
            Text(
                text = clipMeta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (snippet != null) {
            Text(
                text = highlightedAnnotatedString(snippet, query),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (movement.muscles.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                movement.muscles.take(3).forEach { muscle ->
                    val label = muscleLabel(muscle)
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = highlightedAnnotatedString(label, query),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
        if (movement.tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                movement.tags.take(2).forEach { tag ->
                    SuggestionChip(
                        onClick = { onTagClick?.invoke(tag) },
                        label = {
                            Text(
                                text = highlightedAnnotatedString(tag, query),
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

internal fun clipMetaLabel(clips: List<AnimationClip>): String? {
    if (clips.isEmpty()) return null
    val count = clips.size
    val totalSeconds = clips.sumOf { it.durationSeconds.toDouble() }
    return "$count clip${if (count > 1) "s" else ""} · ${"%.1f".format(totalSeconds)}s"
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
