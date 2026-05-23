package com.morphocore.feature.browse.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

private val difficultyColors = mapOf(
    Difficulty.BEGINNER     to Color(0xFF4CAF50),
    Difficulty.INTERMEDIATE to Color(0xFFFF9800),
    Difficulty.ADVANCED     to Color(0xFFF44336)
)

@Composable
fun DisciplineCard(
    discipline: Discipline,
    difficultyBreakdown: Map<Difficulty, Int> = emptyMap(),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = discipline.name,
                    style = MaterialTheme.typography.titleLarge
                )
                if (discipline.description.isNotBlank()) {
                    Text(
                        text = highlightedAnnotatedString(discipline.description, query),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    text = "${discipline.movementIds.size} movements",
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
