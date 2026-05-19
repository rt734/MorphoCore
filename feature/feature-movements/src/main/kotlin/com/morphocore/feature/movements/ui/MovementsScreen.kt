package com.morphocore.feature.movements.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morphocore.domain.Difficulty
import com.morphocore.feature.movements.MovementsUiState
import com.morphocore.feature.movements.MovementsViewModel

private fun Difficulty.displayLabel(): String = when (this) {
    Difficulty.BEGINNER     -> "Beginner"
    Difficulty.INTERMEDIATE -> "Intermediate"
    Difficulty.ADVANCED     -> "Advanced"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsScreen(
    disciplineId: String,
    onMovementSelected: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MovementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val state = uiState) {
                        is MovementsUiState.Ready -> state.disciplineName
                        else -> disciplineId
                    }
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
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
                                    FilterChip(
                                        selected = tag in state.selectedTags,
                                        onClick = { viewModel.toggleTag(tag) },
                                        label = { Text(tag) }
                                    )
                                }
                            }
                        }
                    }
                    items(state.movements, key = { it.id }) { movement ->
                        MovementRow(
                            movement = movement,
                            onClick = { onMovementSelected(movement.id) }
                        )
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
                    Text("Error: ${state.message}")
                }
            }
        }
    }
}
