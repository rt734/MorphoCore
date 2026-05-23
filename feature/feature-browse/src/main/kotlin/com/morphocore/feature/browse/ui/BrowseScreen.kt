package com.morphocore.feature.browse.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morphocore.domain.Difficulty
import com.morphocore.domain.Movement
import com.morphocore.feature.browse.BrowseUiState
import com.morphocore.feature.browse.BrowseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onDisciplineSelected: (String) -> Unit,
    onMovementSelected: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    BackHandler(enabled = query.isNotBlank()) {
        viewModel.setQuery("")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is BrowseUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is BrowseUiState.Ready -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Search bar as pinned first item
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = viewModel::setQuery,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text("Search disciplines and movements…") },
                            singleLine = true,
                            trailingIcon = {
                                if (query.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            }
                        )
                    }

                    if (state.query.isBlank()) {
                        // Stats line: shown only when count is known
                        if (state.totalMovementCount > 0) {
                            item {
                                Text(
                                    text = "${state.disciplines.size} disciplines · ${state.totalMovementCount} movements",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                                )
                            }
                        }
                        // Difficulty filter chips
                        if (state.difficultyBreakdown.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(Difficulty.BEGINNER, Difficulty.INTERMEDIATE, Difficulty.ADVANCED)
                                        .forEach { diff ->
                                            val count = state.difficultyBreakdown[diff] ?: 0
                                            if (count > 0) {
                                                val label = when (diff) {
                                                    Difficulty.BEGINNER     -> "Beginner"
                                                    Difficulty.INTERMEDIATE -> "Intermediate"
                                                    Difficulty.ADVANCED     -> "Advanced"
                                                }
                                                FilterChip(
                                                    selected = state.selectedDifficulty == diff,
                                                    onClick = { viewModel.toggleDifficultyFilter(diff) },
                                                    label = { Text("$count $label") }
                                                )
                                            }
                                        }
                                }
                            }
                        }
                        // Normal mode: show discipline cards with spacing
                        items(state.disciplines, key = { it.id }) { discipline ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                DisciplineCard(
                                    discipline = discipline,
                                    difficultyBreakdown = state.disciplineBreakdowns[discipline.id] ?: emptyMap(),
                                    onClick = { onDisciplineSelected(discipline.id) }
                                )
                            }
                        }
                    } else {
                        // Search mode result summary
                        val resultParts = buildList {
                            if (state.disciplines.isNotEmpty()) add("${state.disciplines.size} discipline${if (state.disciplines.size > 1) "s" else ""}")
                            if (state.movementResults.isNotEmpty()) add("${state.movementResults.size} movement${if (state.movementResults.size > 1) "s" else ""}")
                        }
                        if (resultParts.isNotEmpty()) {
                            item {
                                Text(
                                    text = resultParts.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }
                        // Search mode: matching disciplines, then matching movements
                        if (state.disciplines.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Disciplines",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                            items(state.disciplines, key = { "d-${it.id}" }) { discipline ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                    DisciplineCard(
                                        discipline = discipline,
                                        onClick = { onDisciplineSelected(discipline.id) }
                                    )
                                }
                            }
                        }
                        if (state.movementResults.isNotEmpty()) {
                            item {
                                if (state.disciplines.isNotEmpty()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                                Text(
                                    text = "Movements",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                            items(state.movementResults, key = { "m-${it.id}" }) { movement ->
                                MovementSearchResultRow(
                                    movement = movement,
                                    query = state.query,
                                    onClick = { onMovementSelected(movement.id) }
                                )
                            }
                        }
                        if (state.disciplines.isEmpty() && state.movementResults.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No results for "${state.query}"")
                                }
                            }
                        }
                    }
                }
            }
            is BrowseUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = viewModel::retry) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

internal fun findHighlightRange(text: String, query: String): IntRange? {
    val q = query.trim().lowercase()
    if (q.isBlank()) return null
    val idx = text.lowercase().indexOf(q)
    if (idx == -1) return null
    return idx until idx + q.length
}

internal fun browseDescriptionMatchSnippet(description: String, query: String): String? {
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
private fun MovementSearchResultRow(
    movement: Movement,
    query: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val disciplineLabel = movement.disciplineId
        .replaceFirstChar { it.uppercaseChar() }
        .replace('-', ' ')
    val nameMatches = query.isNotBlank() && findHighlightRange(movement.name, query) != null
    val snippet = if (!nameMatches) browseDescriptionMatchSnippet(movement.description, query) else null

    ListItem(
        headlineContent = { Text(highlightedAnnotatedString(movement.name, query)) },
        supportingContent = {
            if (snippet != null) {
                Column {
                    Text(disciplineLabel)
                    Text(
                        text = highlightedAnnotatedString(snippet, query),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(disciplineLabel)
            }
        },
        trailingContent = {
            val (label, color) = when (movement.difficulty) {
                Difficulty.BEGINNER     -> "Beginner"     to MaterialTheme.colorScheme.secondary
                Difficulty.INTERMEDIATE -> "Intermediate" to MaterialTheme.colorScheme.tertiary
                Difficulty.ADVANCED     -> "Advanced"     to MaterialTheme.colorScheme.error
            }
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}
