package com.morphocore.feature.detail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morphocore.domain.Movement
import com.morphocore.feature.detail.DetailUiState
import com.morphocore.feature.detail.DetailViewModel
import com.morphocore.feature.detail.PlaybackState
import com.morphocore.feature.detail.toSceneEnvironment
import com.morphocore.rendering.sceneview.SceneViewportImpl
import com.morphocore.rendering.sceneview.SceneViewportSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    movementId: String,
    onBack: () -> Unit,
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
            viewModel.onModelLoaded(state.movement.defaultClip)
        }
    }

    LaunchedEffect(playbackState) {
        val clip = playbackState.currentClip
        if (clip.isNotEmpty()) {
            if (playbackState.isPlaying) {
                viewport.play(clip)
            } else {
                viewport.pause()
            }
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
                ) {
                    SceneViewportSurface(
                        viewport = viewport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    PlaybackControls(
                        movement = state.movement,
                        playbackState = playbackState,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onClipSelected = viewModel::selectClip
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
    onClipSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
        }
        if (movement.clips.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(movement.clips) { clip ->
                    FilterChip(
                        selected = clip.name == playbackState.currentClip,
                        onClick = { onClipSelected(clip.name) },
                        label = { Text(clip.name) }
                    )
                }
            }
        }
    }
}
