package com.morphocore.feature.detail.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morphocore.domain.CameraPreset
import com.morphocore.domain.Difficulty
import com.morphocore.domain.Movement
import com.morphocore.domain.MuscleGroup
import com.morphocore.feature.detail.DetailUiState
import com.morphocore.feature.detail.DetailViewModel
import com.morphocore.feature.detail.PlaybackState
import com.morphocore.feature.detail.toSceneEnvironment
import com.morphocore.rendering.sceneview.SceneViewportImpl
import com.morphocore.rendering.sceneview.SceneViewportSurface

private fun String.movementIdToDisplayName(): String =
    substringAfter('.').split('-').joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercaseChar() }
    }

private fun MuscleGroup.displayName(): String = when (this) {
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

private val speedOptions = listOf(
    0.5f to "0.5×",
    1f   to "1×",
    1.5f to "1.5×",
    2f   to "2×"
)

private val cameraOptions = listOf(
    "front"         to "Front",
    "side"          to "Side",
    "top"           to "Top",
    "three_quarter" to "3/4"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    movementId: String,
    onBack: () -> Unit,
    onNavigateToMovement: (String) -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val theme by viewModel.activeTheme.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    val viewport = remember { SceneViewportImpl() }

    LaunchedEffect(theme) {
        viewport.applySceneEnvironment(theme.scene.toSceneEnvironment())
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is DetailUiState.Ready) {
            viewport.loadModel(state.movement.modelPath)
            viewModel.onModelLoaded(state.movement.defaultClip, state.movement.cameraPreset)
        }
    }

    LaunchedEffect(playbackState) {
        val clip = playbackState.currentClip
        if (clip.isNotEmpty()) {
            viewport.setPlaybackSpeed(playbackState.speedMultiplier)
            if (playbackState.isPlaying) {
                viewport.play(clip, loop = playbackState.isLooping)
            } else {
                viewport.pause()
            }
        }
    }

    val cameraPreset = playbackState.cameraPreset
    LaunchedEffect(cameraPreset) {
        if (cameraPreset != null) {
            viewport.setCamera(CameraPreset(cameraPreset))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((uiState as? DetailUiState.Ready)?.movement?.name ?: movementId) },
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
            is DetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is DetailUiState.Ready -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    SceneViewportSurface(
                        viewport = viewport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    )
                    PlaybackControls(
                        movement = state.movement,
                        playbackState = playbackState,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onToggleLoop = viewModel::toggleLoop,
                        onClipSelected = viewModel::selectClip,
                        onSpeedSelected = viewModel::setSpeed,
                        onCameraSelected = viewModel::selectCamera
                    )
                    MovementInfoPanel(
                        movement = state.movement,
                        relatedMovements = state.relatedMovements,
                        onNavigateToMovement = onNavigateToMovement
                    )
                }
            }
            is DetailUiState.Error -> {
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

@Composable
private fun PlaybackControls(
    movement: Movement,
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onToggleLoop: () -> Unit,
    onClipSelected: (String) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onCameraSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Play / pause + loop toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = onTogglePlayPause) {
                if (playbackState.isPlaying) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                }
            }
            IconButton(onClick = onToggleLoop) {
                if (playbackState.isLooping) {
                    Icon(Icons.Default.Repeat, contentDescription = "Loop on")
                } else {
                    Icon(Icons.Default.RepeatOne, contentDescription = "Loop off")
                }
            }
        }

        // Speed row
        Text(
            text = "Speed",
            style = MaterialTheme.typography.labelSmall
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(speedOptions) { (speed, label) ->
                FilterChip(
                    selected = playbackState.speedMultiplier == speed,
                    onClick = { onSpeedSelected(speed) },
                    label = { Text(label) }
                )
            }
        }

        // Camera row
        Text(
            text = "Camera",
            style = MaterialTheme.typography.labelSmall
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cameraOptions) { (key, label) ->
                FilterChip(
                    selected = playbackState.cameraPreset == key,
                    onClick = { onCameraSelected(key) },
                    label = { Text(label) }
                )
            }
        }

        // Clip selector (only when more than one clip)
        if (movement.clips.size > 1) {
            Text(
                text = "Clips",
                style = MaterialTheme.typography.labelSmall
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(movement.clips) { clip ->
                    FilterChip(
                        selected = clip.name == playbackState.currentClip,
                        onClick = { onClipSelected(clip.name) },
                        label = {
                            val name = clip.name.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
                            Text("$name · ${"%.1fs".format(clip.durationSeconds)}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MovementInfoPanel(
    movement: Movement,
    relatedMovements: List<com.morphocore.domain.Movement>,
    onNavigateToMovement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Difficulty row
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Difficulty", style = MaterialTheme.typography.labelSmall)
            val (label, color) = when (movement.difficulty) {
                Difficulty.BEGINNER     -> "Beginner"     to Color(0xFF4CAF50)
                Difficulty.INTERMEDIATE -> "Intermediate" to Color(0xFFFF9800)
                Difficulty.ADVANCED     -> "Advanced"     to Color(0xFFF44336)
            }
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = color,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Clip summary (only when multiple clips)
        if (movement.clips.size > 1) {
            val totalSeconds = movement.clips.sumOf { it.durationSeconds.toDouble() }.toFloat()
            Text(
                text = "${"%.1f".format(totalSeconds)}s total · ${movement.clips.size} clips",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tags section
        if (movement.tags.isNotEmpty()) {
            Text("Tags", style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                movement.tags.forEach { tag ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(tag) }
                    )
                }
            }
        }

        // Muscles section
        if (movement.muscles.isNotEmpty()) {
            Text("Muscles", style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                movement.muscles.forEach { muscle ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(muscle.displayName()) }
                    )
                }
            }
        }

        // Common mistakes section
        if (movement.commonMistakes.isNotEmpty()) {
            Text("Common Mistakes", style = MaterialTheme.typography.labelSmall)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                movement.commonMistakes.forEach { mistake ->
                    Text(
                        text = "• $mistake",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Prerequisites section
        if (movement.prerequisites.isNotEmpty()) {
            Text("Prerequisites", style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                movement.prerequisites.forEach { prereqId ->
                    SuggestionChip(
                        onClick = { onNavigateToMovement(prereqId) },
                        label = { Text(prereqId.movementIdToDisplayName()) }
                    )
                }
            }
        }

        // Related movements
        if (relatedMovements.isNotEmpty()) {
            Text("Related", style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                relatedMovements.forEach { related ->
                    SuggestionChip(
                        onClick = { onNavigateToMovement(related.id) },
                        label = { Text(related.name) }
                    )
                }
            }
        }
    }
}
