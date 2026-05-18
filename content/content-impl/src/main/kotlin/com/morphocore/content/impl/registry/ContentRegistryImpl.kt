package com.morphocore.content.impl.registry

import com.morphocore.content.api.AssetSource
import com.morphocore.content.api.ContentError
import com.morphocore.content.api.ContentRegistry
import com.morphocore.content.api.RegistryState
import com.morphocore.domain.Discipline
import com.morphocore.domain.Movement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal data class ContentIndex(
    val disciplines: Map<String, Discipline> = emptyMap(),
    val movements: Map<String, Movement> = emptyMap()
)

class ContentRegistryImpl(
    private val sources: List<AssetSource>,
    private val ioDispatcher: CoroutineDispatcher,
    // scope is retained for future DI wiring (Hilt will call refresh() via applicationScope)
    @Suppress("UnusedPrivateMember") private val scope: CoroutineScope
) : ContentRegistry {

    private val _state = MutableStateFlow<RegistryState>(RegistryState.Loading)
    override val state: StateFlow<RegistryState> = _state.asStateFlow()

    private val _index = MutableStateFlow(ContentIndex())
    internal val index: StateFlow<ContentIndex> = _index.asStateFlow()

    override suspend fun refresh() {
        _state.value = RegistryState.Loading
        withContext(ioDispatcher) {
            val allDisciplines = mutableMapOf<String, Discipline>()
            val allMovements = mutableMapOf<String, Movement>()

            for (source in sources) {
                val result = scanSource(source)
                result.disciplines.forEach { allDisciplines[it.id] = it }
                result.movements.forEach { allMovements[it.id] = it }
            }

            _state.value = if (allDisciplines.isEmpty()) {
                RegistryState.Error(ContentError.NoContentFound)
            } else {
                _index.value = ContentIndex(allDisciplines, allMovements)
                RegistryState.Ready
            }
        }
    }
}
