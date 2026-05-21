package com.morphocore.feature.browse

import com.morphocore.content.api.ContentError
import com.morphocore.content.api.ContentRegistry
import com.morphocore.content.api.RegistryState
import com.morphocore.content.testing.FakeContentRepository
import com.morphocore.domain.Difficulty
import com.morphocore.domain.Discipline
import com.morphocore.domain.Movement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun discipline(id: String, name: String) =
        Discipline(id = id, name = name, iconPath = null, movementIds = emptyList())

    private fun movement(disciplineId: String, slug: String) =
        Movement(
            id = "$disciplineId.$slug",
            disciplineId = disciplineId,
            name = slug.replace("_", " "),
            modelPath = "models/$disciplineId/$slug.glb",
            defaultClip = "idle",
            clips = emptyList(),
            muscles = emptyList(),
            difficulty = Difficulty.BEGINNER,
            tags = emptyList(),
            cameraPreset = null,
            prerequisites = emptyList(),
            commonMistakes = emptyList()
        )

    private fun vm(
        repo: FakeContentRepository = FakeContentRepository(),
        registry: FakeContentRegistry = FakeContentRegistry()
    ) = BrowseViewModel(repo, registry)

    // ── baseline ──────────────────────────────────────────────────────────

    @Test
    fun `uiState is Ready with empty list when repository emits nothing`() = runTest {
        val vm = vm()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<BrowseUiState.Ready>(vm.uiState.value)
        assertEquals(emptyList(), state.disciplines)
    }

    @Test
    fun `uiState is Ready when repository emits disciplines`() = runTest {
        val repo = FakeContentRepository(disciplines = listOf(discipline("karate", "Karate")))
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertIs<BrowseUiState.Ready>(vm.uiState.value)
    }

    @Test
    fun `Ready state contains all disciplines from repository`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate"), discipline("yoga", "Yoga"))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(2, state.disciplines.size)
    }

    @Test
    fun `disciplines are in order emitted by repository`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("yoga", "Yoga"), discipline("karate", "Karate"))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals("yoga", state.disciplines.first().id)
    }

    @Test
    fun `uiState is Error when repository throws`() = runTest {
        val repo = FakeContentRepository(throwOnDisciplines = RuntimeException("load failed"))
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<BrowseUiState.Error>(vm.uiState.value)
        assertEquals("load failed", state.message)
    }

    @Test
    fun `uiState is Error when registry emits error state`() = runTest {
        val registry = FakeContentRegistry(initialState = RegistryState.Loading)
        val vm = vm(registry = registry)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        registry.setState(RegistryState.Error(ContentError.NoContentFound))
        advanceUntilIdle()
        assertIs<BrowseUiState.Error>(vm.uiState.value)
    }

    @Test
    fun `retry calls contentRegistry refresh`() = runTest {
        var refreshCalled = false
        val registry = object : FakeContentRegistry() {
            override suspend fun refresh() { refreshCalled = true }
        }
        val vm = vm(registry = registry)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.retry()
        advanceUntilIdle()
        assertEquals(true, refreshCalled)
    }

    // ── totalMovementCount ────────────────────────────────────────────────

    @Test
    fun `totalMovementCount is zero when repository has no movements`() = runTest {
        val vm = vm()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(0, state.totalMovementCount)
    }

    @Test
    fun `totalMovementCount reflects all movements in repository`() = runTest {
        val repo = FakeContentRepository(
            movementsById = mapOf(
                "karate.front_kick" to movement("karate", "front_kick"),
                "yoga.downward_dog" to movement("yoga", "downward_dog"),
                "gym.squat" to movement("gym", "squat")
            )
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(3, state.totalMovementCount)
    }

    @Test
    fun `totalMovementCount is preserved in search mode`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsById = mapOf(
                "karate.front_kick" to movement("karate", "front_kick"),
                "karate.roundhouse" to movement("karate", "roundhouse")
            )
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setQuery("yoga")
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(2, state.totalMovementCount)
    }
}

private open class FakeContentRegistry(
    initialState: RegistryState = RegistryState.Ready
) : ContentRegistry {
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<RegistryState> = _state.asStateFlow()
    override suspend fun refresh() {}
    fun setState(s: RegistryState) { _state.value = s }
}
