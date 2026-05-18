package com.morphocore.content.testing

import com.morphocore.content.api.ContentRepository
import com.morphocore.domain.Discipline
import com.morphocore.domain.Movement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class FakeContentRepository(
    private val disciplines: List<Discipline> = emptyList(),
    private val movementsByDiscipline: Map<String, List<Movement>> = emptyMap(),
    private val movementsById: Map<String, Movement> = emptyMap(),
    private val throwOnDisciplines: Exception? = null,
    private val throwOnMovements: Exception? = null
) : ContentRepository {

    override fun observeDisciplines(): Flow<List<Discipline>> =
        if (throwOnDisciplines != null) flow { throw throwOnDisciplines }
        else flowOf(disciplines)

    override fun observeMovements(disciplineId: String): Flow<List<Movement>> =
        if (throwOnMovements != null) flow { throw throwOnMovements }
        else flowOf(movementsByDiscipline[disciplineId] ?: emptyList())

    override suspend fun getMovement(movementId: String): Movement? = movementsById[movementId]
}
