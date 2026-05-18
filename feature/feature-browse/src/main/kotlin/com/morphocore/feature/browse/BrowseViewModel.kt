package com.morphocore.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morphocore.content.api.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val contentRepository: ContentRepository
) : ViewModel() {

    val uiState: StateFlow<BrowseUiState> = contentRepository
        .observeDisciplines()
        .map { disciplines ->
            if (disciplines.isEmpty()) BrowseUiState.Loading
            else BrowseUiState.Ready(disciplines)
        }
        .catch { e -> emit(BrowseUiState.Error(e.message ?: "Failed to load disciplines")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BrowseUiState.Loading
        )
}
