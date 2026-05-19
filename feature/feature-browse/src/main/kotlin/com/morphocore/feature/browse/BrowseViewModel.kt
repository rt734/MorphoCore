package com.morphocore.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morphocore.content.api.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val uiState: StateFlow<BrowseUiState> = combine(
        contentRepository.observeDisciplines(),
        contentRepository.observeAllMovements(),
        _query.debounce(300)
    ) { disciplines, movements, query ->
        if (query.isBlank()) {
            BrowseUiState.Ready(disciplines = disciplines)
        } else {
            val q = query.trim().lowercase()
            val matchingDisciplines = disciplines.filter { it.name.lowercase().contains(q) }
            val matchingMovements = movements.filter { m ->
                m.name.lowercase().contains(q) || m.tags.any { it.lowercase().contains(q) }
            }
            BrowseUiState.Ready(matchingDisciplines, matchingMovements, query)
        }
    }
        .catch { e -> emit(BrowseUiState.Error(e.message ?: "Failed to load disciplines")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BrowseUiState.Loading
        )

    fun setQuery(query: String) {
        _query.value = query
    }
}
