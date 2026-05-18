package com.morphocore.content.impl.registry

import com.morphocore.content.api.ContentError
import com.morphocore.content.api.RegistryState
import com.morphocore.content.impl.FakeAssetSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ContentRegistryImplTest {

    private val karateJson = checkNotNull(javaClass.getResourceAsStream("/fixtures/karate-sample.json"))
        .bufferedReader().readText()
    private val yogaJson = checkNotNull(javaClass.getResourceAsStream("/fixtures/yoga-sample.json"))
        .bufferedReader().readText()

    @Test
    fun `initial state is Loading`() = runTest {
        val registry = ContentRegistryImpl(
            sources = listOf(FakeAssetSource(manifests = mapOf("karate" to karateJson))),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            scope = this
        )
        assertEquals(RegistryState.Loading, registry.state.value)
    }

    @Test
    fun `state is Ready after refresh with content`() = runTest {
        val registry = ContentRegistryImpl(
            sources = listOf(FakeAssetSource(manifests = mapOf("karate" to karateJson))),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            scope = this
        )
        registry.refresh()
        advanceUntilIdle()
        assertEquals(RegistryState.Ready, registry.state.value)
    }

    @Test
    fun `state is Error NoContentFound when all sources empty`() = runTest {
        val registry = ContentRegistryImpl(
            sources = listOf(FakeAssetSource(manifests = emptyMap())),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            scope = this
        )
        registry.refresh()
        advanceUntilIdle()
        val state = registry.state.value
        assertIs<RegistryState.Error>(state)
        assertEquals(ContentError.NoContentFound, state.cause)
    }

    @Test
    fun `index has disciplines after successful refresh`() = runTest {
        val registry = ContentRegistryImpl(
            sources = listOf(FakeAssetSource(manifests = mapOf("karate" to karateJson))),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            scope = this
        )
        registry.refresh()
        advanceUntilIdle()
        assertEquals(1, registry.index.value.disciplines.size)
        assertTrue(registry.index.value.disciplines.containsKey("karate"))
    }

    @Test
    fun `index merges disciplines from multiple sources`() = runTest {
        val registry = ContentRegistryImpl(
            sources = listOf(
                FakeAssetSource(id = "src1", manifests = mapOf("karate" to karateJson)),
                FakeAssetSource(id = "src2", manifests = mapOf("yoga" to yogaJson))
            ),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            scope = this
        )
        registry.refresh()
        advanceUntilIdle()
        assertEquals(2, registry.index.value.disciplines.size)
        assertEquals(RegistryState.Ready, registry.state.value)
    }

    @Test
    fun `refresh resets state to Loading before scanning`() = runTest {
        val registry = ContentRegistryImpl(
            sources = listOf(FakeAssetSource(manifests = mapOf("karate" to karateJson))),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            scope = this
        )
        registry.refresh()
        advanceUntilIdle()
        assertEquals(RegistryState.Ready, registry.state.value)

        registry.refresh()
        assertEquals(RegistryState.Loading, registry.state.value)
        advanceUntilIdle()
        assertEquals(RegistryState.Ready, registry.state.value)
    }
}
