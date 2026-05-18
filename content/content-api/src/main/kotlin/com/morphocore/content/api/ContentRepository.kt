package com.morphocore.content.api

import com.morphocore.domain.Discipline
import com.morphocore.domain.Movement
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    fun observeDisciplines(): Flow<List<Discipline>>
    fun observeMovements(disciplineId: String): Flow<List<Movement>>
    suspend fun getMovement(movementId: String): Movement?
}
