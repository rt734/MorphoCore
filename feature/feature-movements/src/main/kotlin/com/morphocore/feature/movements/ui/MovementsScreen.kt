package com.morphocore.feature.movements.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morphocore.domain.Difficulty
import com.morphocore.domain.MuscleGroup
import com.morphocore.feature.movements.MovementsSort
import com.morphocore.feature.movements.MovementsUiState
import com.morphocore.feature.movements.MovementsViewModel

private fun Difficulty.displayLabel(): String = when (this) {
    Difficulty.BEGINNER     -> "Beginner"
    Difficulty.INTERMEDIATE -> "Intermediate"
    Difficulty.ADVANCED     -> "Advanced"
}

private fun MuscleGroup.displayLabel(): String = when (this) {
    MuscleGroup.Quadriceps  -> "Quads"
    MuscleGroup.Hamstrings  -> "Hamstrings"
    MuscleGroup.Glutes      -> "Glutes"
    MuscleGroup.Core        -> "Core"
    MuscleGroup.Shoulders   -> "Shoulders"
    MuscleGroup.Back        -> "Back"
    MuscleGroup.Chest       -> "Chest"
    MuscleGroup.Calves      -> "Calves"
    MuscleGroup.HipFlexors  -> "Hip Flexors"
    is MuscleGroup.Unknown  -> this.raw.replaceFirstChar { it.uppercaseChar() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MovementsScreen(
    disciplineId: String,
    onMovementSelected: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MovementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    val filtersActive = (uiState as? MovementsUiState.Ready)
        ?.let { it.selectedTags.isNotEmpty() || it.selectedDifficulties.isNotEmpty() || it.selectedMuscles.isNotEmpty() } == true

    BackHandler(enabled = query.isNotBlank()) {
        viewModel.setQuery("")
    }
    BackHandler(enabled = filtersActive && query.isBlank()) {
        viewModel.clearFilters()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (val state = uiState) {
                        is MovementsUiState.Ready -> Column {
                            Text(state.disciplineName, style = MaterialTheme.typography.titleLarge)
                            if (state.totalCount > 0) {
                                Text(
                                    "${state.totalCount} movements",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        else -> Text(disciplineId)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState is MovementsUiState.Ready) {
                        val sortLabel = when ((uiState as MovementsUiState.Ready).sort) {
                            MovementsSort.BY_DIFFICULTY -> "By difficulty"
                            MovementsSort.BY_NAME -> "By name"
                        }
                        IconButton(onClick = viewModel::toggleSort) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = sortLabel
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is MovementsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is MovementsUiState.Ready -> {
                val filtersActive = state.selectedTags.isNotEmpty() || state.selectedDifficulties.isNotEmpty() || state.selectedMuscles.isNotEmpty()
                val sortNonDefault = state.sort == MovementsSort.BY_NAME

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Discipline description header
                    if (state.disciplineDescription.isNotBlank()) {
                        item {
                            Text(
                                text = state.disciplineDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    // Search field
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = viewModel::setQuery,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text("Search ${state.disciplineName} movements…") },
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
                    // Difficulty chips
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Difficulty.entries.forEach { difficulty ->
                                FilterChip(
                                    selected = difficulty in state.selectedDifficulties,
                                    onClick = { viewModel.toggleDifficulty(difficulty) },
                                    label = { Text(difficulty.displayLabel()) }
                                )
                            }
                        }
                    }
                    // Muscle chips
                    if (state.availableMuscles.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.availableMuscles.forEach { muscle ->
                                    FilterChip(
                                        selected = muscle in state.selectedMuscles,
                                        onClick = { viewModel.toggleMuscle(muscle) },
                                        label = { Text(muscle.displayLabel()) }
                                    )
                                }
                            }
                        }
                    }
                    // Tag chips
                    if (state.availableTags.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.availableTags.forEach { tag ->
                                    val count = state.tagCounts[tag]
                                    FilterChip(
                                        selected = tag in state.selectedTags,
                                        onClick = { viewModel.toggleTag(tag) },
                                        label = {
                                            Text(if (count != null) "$tag ($count)" else tag)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    // Movement count stat
                    if (state.totalCount > 0) {
                        item {
                            val countText = if (state.movements.size < state.totalCount)
                                "Showing ${state.movements.size} of ${state.totalCount} movements"
                            else
                                "${state.totalCount} movements"
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                            )
                        }
                    }
                    // Difficulty breakdown (pre-filter, always reflects full discipline)
                    if (state.difficultyBreakdown.isNotEmpty()) {
                        item {
                            val parts = listOf(Difficulty.BEGINNER, Difficulty.INTERMEDIATE, Difficulty.ADVANCED)
                                .mapNotNull { d ->
                                    state.difficultyBreakdown[d]?.let { count ->
                                        val label = when (d) {
                                            Difficulty.BEGINNER -> "Beginner"
                                            Difficulty.INTERMEDIATE -> "Intermediate"
                                            Difficulty.ADVANCED -> "Advanced"
                                        }
                                        "$count $label"
                                    }
                                }
                            Text(
                                text = parts.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
                            )
                        }
                    }
                    // Active sort / filter summary
                    if (sortNonDefault || filtersActive) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val parts = buildList {
                                    if (sortNonDefault) add("Sorted by name")
                                    val filterCount = state.selectedTags.size + state.selectedDifficulties.size + state.selectedMuscles.size
                                    if (filterCount > 0) add("$filterCount filter${if (filterCount > 1) "s" else ""} active")
                                }
                                Text(
                                    text = parts.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (filtersActive) {
                                    TextButton(
                                        onClick = viewModel::clearFilters,
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Text("Clear")
                                    }
                                }
                            }
                        }
                    }
                    // Empty state
                    if (state.movements.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val message = when {
                                    query.isNotBlank() -> "No movements match \"$query\""
                                    filtersActive -> "No movements match your filters"
                                    else -> "No movements in this discipline yet"
                                }
                                Text(text = message, style = MaterialTheme.typography.bodyMedium)
                                if (filtersActive) {
                                    TextButton(onClick = viewModel::clearFilters) {
                                        Text("Clear Filters")
                                    }
                                }
                            }
                        }
                    } else {
                        if (state.sort == MovementsSort.BY_DIFFICULTY) {
                            difficultyGroupedItems(
                                movements = state.movements,
                                onMovementSelected = onMovementSelected,
                                onTagClick = viewModel::toggleTag,
                                query = query
                            )
                        } else {
                            items(state.movements, key = { it.id }) { movement ->
                                MovementRow(
                                    movement = movement,
                                    onClick = { onMovementSelected(movement.id) },
                                    onTagClick = viewModel::toggleTag,
                                    query = query,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
            is MovementsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
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

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.difficultyGroupedItems(
    movements: List<com.morphocore.domain.Movement>,
    onMovementSelected: (String) -> Unit,
    onTagClick: (String) -> Unit,
    query: String
) {
    val groups = movements.groupBy { it.difficulty }
    listOf(
        com.morphocore.domain.Difficulty.BEGINNER,
        com.morphocore.domain.Difficulty.INTERMEDIATE,
        com.morphocore.domain.Difficulty.ADVANCED
    ).forEach { difficulty ->
        val group = groups[difficulty] ?: return@forEach
        stickyHeader(key = "header_${difficulty.name}") {
            val label = when (difficulty) {
                com.morphocore.domain.Difficulty.BEGINNER     -> "Beginner"
                com.morphocore.domain.Difficulty.INTERMEDIATE -> "Intermediate"
                com.morphocore.domain.Difficulty.ADVANCED     -> "Advanced"
            }
            Row(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${group.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(group, key = { it.id }) { movement ->
            MovementRow(
                movement = movement,
                onClick = { onMovementSelected(movement.id) },
                onTagClick = onTagClick,
                query = query,
                modifier = androidx.compose.ui.Modifier.animateItem()
            )
        }
    }
}
