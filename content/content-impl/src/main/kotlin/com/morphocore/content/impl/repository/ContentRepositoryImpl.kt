package com.morphocore.content.impl.repository

import com.morphocore.content.api.ContentRepository
import com.morphocore.content.impl.registry.ContentRegistryImpl
import com.morphocore.domain.Discipline
import com.morphocore.domain.Movement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContentRepositoryImpl(private val registry: ContentRegistryImpl) : ContentRepository {

    override fun observeDisciplines(): Flow<List<Discipline>> =
        registry.index.map { index ->
            index.disciplines.values.sortedBy { it.name }
        }

    override fun observeMovements(disciplineId: String): Flow<List<Movement>> =
        registry.index.map { index ->
            index.movements.values
                .filter { it.disciplineId == disciplineId }
                .sortedWith(compareBy({ it.difficulty.ordinal }, { it.name }))
        }

    override fun observeAllMovements(): Flow<List<Movement>> =
        registry.index.map { index -> index.movements.values.sortedBy { it.name } }

    override suspend fun getMovement(movementId: String): Movement? =
        registry.index.value.movements[movementId]
}
