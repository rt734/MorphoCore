package com.morphocore.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morphocore.content.api.ContentRepository
import com.morphocore.domain.Theme
import com.morphocore.preferences.api.UserPreferences
import com.morphocore.theme.api.ThemeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val themeProvider: ThemeProvider,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val movementId: String = checkNotNull(savedStateHandle["movementId"])

    val uiState: StateFlow<DetailUiState> = flow {
        val movement = contentRepository.getMovement(movementId)
        if (movement == null) {
            emit(DetailUiState.Error("Movement not found: $movementId"))
            return@flow
        }
        val excludedIds = (movement.prerequisites + movement.id).toSet()
        emitAll(
            combine(
                contentRepository.observeMovements(movement.disciplineId),
                contentRepository.observeAllMovements()
            ) { disciplineMovements, allMovements ->
                val related = disciplineMovements
                    .filter { it.id !in excludedIds }
                    .filter { candidate -> candidate.tags.any { it in movement.tags } }
                    .take(3)
                val unlocked = disciplineMovements
                    .filter { movement.id in it.prerequisites }
                    .take(3)
                val prerequisites = movement.prerequisites
                    .mapNotNull { prereqId -> disciplineMovements.find { it.id == prereqId } }
                val crossDisciplineRelated = allMovements
                    .filter { it.disciplineId != movement.disciplineId }
                    .filter { it.tags.any { t -> t in movement.tags } }
                    .take(3)
                DetailUiState.Ready(movement, related, unlocked, prerequisites, crossDisciplineRelated)
            }
        )
    }
        .catch { e -> emit(DetailUiState.Error(e.message ?: "Failed to load movement")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState.Loading
        )

    val activeTheme: StateFlow<Theme> = themeProvider.activeTheme

    private val _playbackState = MutableStateFlow(PlaybackState("", false))
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun onModelLoaded(defaultClip: String, defaultCamera: String? = null) {
        _playbackState.value = PlaybackState(
            currentClip = defaultClip,
            isPlaying = true,
            speedMultiplier = userPreferences.getDefaultSpeed(),
            cameraPreset = defaultCamera ?: userPreferences.getDefaultCamera()
        )
    }

    fun togglePlayPause() {
        _playbackState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun selectClip(clipName: String) {
        _playbackState.update { it.copy(currentClip = clipName, isPlaying = true) }
    }

    fun setSpeed(speed: Float) {
        userPreferences.setDefaultSpeed(speed)
        _playbackState.update { it.copy(speedMultiplier = speed) }
    }

    fun selectCamera(presetName: String) {
        userPreferences.setDefaultCamera(presetName)
        _playbackState.update { it.copy(cameraPreset = presetName) }
    }

    fun toggleLoop() {
        _playbackState.update { it.copy(isLooping = !it.isLooping) }
    }
}
