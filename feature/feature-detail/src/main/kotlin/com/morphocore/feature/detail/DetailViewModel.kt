package com.morphocore.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morphocore.content.api.ContentRepository
import com.morphocore.domain.Theme
import com.morphocore.theme.api.ThemeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val themeProvider: ThemeProvider
) : ViewModel() {

    private val movementId: String = checkNotNull(savedStateHandle["movementId"])

    val uiState: StateFlow<DetailUiState> = flow {
        val m = contentRepository.getMovement(movementId)
        if (m != null) {
            emit(DetailUiState.Ready(m))
        } else {
            emit(DetailUiState.Error("Movement not found: $movementId"))
        }
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

    fun onModelLoaded(defaultClip: String, defaultCamera: String?) {
        _playbackState.value = PlaybackState(
            currentClip = defaultClip,
            isPlaying = true,
            speedMultiplier = 1f,
            cameraPreset = defaultCamera
        )
    }

    fun togglePlayPause() {
        _playbackState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun selectClip(clipName: String) {
        _playbackState.update { it.copy(currentClip = clipName, isPlaying = true) }
    }

    fun setSpeed(speed: Float) {
        _playbackState.update { it.copy(speedMultiplier = speed) }
    }

    fun selectCamera(presetName: String) {
        _playbackState.update { it.copy(cameraPreset = presetName) }
    }
}
