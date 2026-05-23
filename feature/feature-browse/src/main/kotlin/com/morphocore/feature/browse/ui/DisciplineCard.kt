package com.morphocore.feature.browse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.morphocore.domain.Difficulty
import com.morphocore.domain.Discipline

private fun findHighlightRange(text: String, query: String): IntRange? {
    val q = query.trim().lowercase()
    if (q.isBlank()) return null
    val idx = text.lowercase().indexOf(q)
    if (idx == -1) return null
    return idx until idx + q.length
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

private val disciplineAvatarPalette = listOf(
    Color(0xFF5C6BC0), // indigo
    Color(0xFF26A69A), // teal
    Color(0xFFEF5350), // red
    Color(0xFF66BB6A), // green
    Color(0xFFFFA726), // orange
    Color(0xFF8D6E63), // brown
    Color(0xFF42A5F5), // blue
    Color(0xFFAB47BC)  // purple
)

private fun disciplineAvatarColor(id: String): Color {
    val index = (id.hashCode() and Int.MAX_VALUE) % disciplineAvatarPalette.size
    return disciplineAvatarPalette[index]
}

internal fun disciplineInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+"))
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        words[0].length >= 2 -> words[0].take(2).uppercase()
        else -> words[0].uppercase()
    }
}

@Composable
private fun DisciplineAvatar(disciplineId: String, disciplineName: String) {
    val bg = disciplineAvatarColor(disciplineId)
    val contentColor = if (bg.luminance() > 0.4f) Color.Black else Color.White
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg)
    ) {
        Text(
            text = disciplineInitials(disciplineName),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

private val difficultyColors = mapOf(
    Difficulty.BEGINNER     to Color(0xFF4CAF50),
    Difficulty.INTERMEDIATE to Color(0xFFFF9800),
    Difficulty.ADVANCED     to Color(0xFFF44336)
)

@Composable
fun DisciplineCard(
    discipline: Discipline,
    difficultyBreakdown: Map<Difficulty, Int> = emptyMap(),
    filteredMovementCount: Int? = null,
    query: String = "",
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DisciplineAvatar(discipline.id, discipline.name)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlightedAnnotatedString(discipline.name, query),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (discipline.description.isNotBlank()) {
                    val nameMatches = query.isNotBlank() &&
                        findHighlightRange(discipline.name, query) != null
                    val displayDescription = if (!nameMatches)
                        browseDescriptionMatchSnippet(discipline.description, query)
                            ?: discipline.description
                    else
                        discipline.description
                    Text(
                        text = highlightedAnnotatedString(displayDescription, query),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                val total = discipline.movementIds.size
                val countText = if (filteredMovementCount != null && filteredMovementCount != total)
                    "$filteredMovementCount of $total movements"
                else
                    "$total movements"
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (difficultyBreakdown.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(Difficulty.BEGINNER, Difficulty.INTERMEDIATE, Difficulty.ADVANCED)
                            .forEach { diff ->
                                val count = difficultyBreakdown[diff] ?: 0
                                if (count > 0) {
                                    Text(
                                        text = "$count ${when (diff) {
                                            Difficulty.BEGINNER     -> "B"
                                            Difficulty.INTERMEDIATE -> "I"
                                            Difficulty.ADVANCED     -> "A"
                                        }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = difficultyColors[diff] ?: MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
