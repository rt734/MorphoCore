package com.morphocore.content.impl.repository

import com.morphocore.content.impl.FakeAssetSource
import com.morphocore.content.impl.registry.ContentRegistryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ContentRepositoryImplTest {

    private val karateJson = checkNotNull(javaClass.getResourceAsStream("/fixtures/karate-sample.json"))
        .bufferedReader().readText()
    private val yogaJson = checkNotNull(javaClass.getResourceAsStream("/fixtures/yoga-sample.json"))
        .bufferedReader().readText()

    private fun buildRegistry(manifests: Map<String, String>, dispatcher: kotlinx.coroutines.CoroutineDispatcher, scope: kotlinx.coroutines.CoroutineScope) =
        ContentRegistryImpl(
            sources = listOf(FakeAssetSource(manifests = manifests)),
            ioDispatcher = dispatcher,
            scope = scope
        )

    @Test
    fun `observeDisciplines emits empty list before refresh`() = runTest {
        val registry = buildRegistry(mapOf("karate" to karateJson), StandardTestDispatcher(testScheduler), this)
        val repo = ContentRepositoryImpl(registry)
        assertTrue(repo.observeDisciplines().first().isEmpty())
    }

    @Test
    fun `observeDisciplines emits disciplines after refresh`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val registry = buildRegistry(mapOf("karate" to karateJson, "yoga" to yogaJson), dispatcher, this)
        val repo = ContentRepositoryImpl(registry)
        registry.refresh()
        advanceUntilIdle()
        assertEquals(2, repo.observeDisciplines().first().size)
    }

    @Test
    fun `observeDisciplines result is sorted by name`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val registry = buildRegistry(mapOf("karate" to karateJson, "yoga" to yogaJson), dispatcher, this)
        val repo = ContentRepositoryImpl(registry)
        registry.refresh()
        advanceUntilIdle()
        val names = repo.observeDisciplines().first().map { it.name }
        assertEquals(names.sorted(), names)
    }

    @Test
    fun `observeMovements filters by disciplineId`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val registry = buildRegistry(mapOf("karate" to karateJson, "yoga" to yogaJson), dispatcher, this)
        val repo = ContentRepositoryImpl(registry)
        registry.refresh()
        advanceUntilIdle()
        val karateMovements = repo.observeMovements("karate").first()
        assertEquals(3, karateMovements.size)
        assertTrue(karateMovements.all { it.disciplineId == "karate" })
    }

    @Test
    fun `observeMovements result is sorted by difficulty then name`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val registry = buildRegistry(mapOf("karate" to karateJson), dispatcher, this)
        val repo = ContentRepositoryImpl(registry)
        registry.refresh()
        advanceUntilIdle()
        val movements = repo.observeMovements("karate").first()
        val diffOrdinals = movements.map { it.difficulty.ordinal }
        assertEquals(diffOrdinals.sorted(), diffOrdinals)
    }

    @Test
    fun `getMovement returns null for unknown id`() = runTest {
        val registry = buildRegistry(emptyMap(), StandardTestDispatcher(testScheduler), this)
        val repo = ContentRepositoryImpl(registry)
        assertNull(repo.getMovement("nonexistent.move"))
    }

    @Test
    fun `getMovement returns correct movement after refresh`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val registry = buildRegistry(mapOf("karate" to karateJson), dispatcher, this)
        val repo = ContentRepositoryImpl(registry)
        registry.refresh()
        advanceUntilIdle()
        val movement = repo.getMovement("karate.roundhouse_kick")
        assertNotNull(movement)
        assertEquals("Roundhouse Kick", movement.name)
    }

    @Test
    fun `observeAllMovements emits empty list before refresh`() = runTest {
        val registry = buildRegistry(mapOf("karate" to karateJson), StandardTestDispatcher(testScheduler), this)
        val repo = ContentRepositoryImpl(registry)
        assertTrue(repo.observeAllMovements().first().isEmpty())
    }

    @Test
    fun `observeAllMovements emits movements from all disciplines`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val registry = buildRegistry(mapOf("karate" to karateJson, "yoga" to yogaJson), dispatcher, this)
        val repo = ContentRepositoryImpl(registry)
        registry.refresh()
        advanceUntilIdle()
        val all = repo.observeAllMovements().first()
        // karate fixture has 3 movements, yoga fixture has 2
        assertEquals(5, all.size)
        assertTrue(all.any { it.disciplineId == "karate" })
        assertTrue(all.any { it.disciplineId == "yoga" })
    }

    @Test
    fun `observeAllMovements result is sorted by name`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val registry = buildRegistry(mapOf("karate" to karateJson, "yoga" to yogaJson), dispatcher, this)
        val repo = ContentRepositoryImpl(registry)
        registry.refresh()
        advanceUntilIdle()
        val names = repo.observeAllMovements().first().map { it.name }
        assertEquals(names.sorted(), names)
    }
}
