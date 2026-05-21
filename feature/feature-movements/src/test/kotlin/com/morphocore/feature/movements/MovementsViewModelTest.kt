package com.morphocore.feature.movements

import androidx.lifecycle.SavedStateHandle
import com.morphocore.content.testing.FakeContentRepository
import com.morphocore.domain.Difficulty
import com.morphocore.domain.Discipline
import com.morphocore.domain.Movement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class MovementsViewModelTest {

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

    private fun savedState(disciplineId: String) =
        SavedStateHandle(mapOf("disciplineId" to disciplineId))

    // ── baseline ──────────────────────────────────────────────────────────

    @Test
    fun `uiState is Ready with empty movements when repository has no movements`() = runTest {
        val vm = MovementsViewModel(savedState("karate"), FakeContentRepository())
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(emptyList(), state.movements)
    }

    @Test
    fun `uiState is Ready when movements are loaded`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(movement("karate", "roundhouse_kick")))
        )
        val vm = MovementsViewModel(savedState("karate"), repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertIs<MovementsUiState.Ready>(vm.uiState.value)
    }

    @Test
    fun `disciplineName is taken from discipline name field`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(movement("karate", "front_kick")))
        )
        val vm = MovementsViewModel(savedState("karate"), repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as MovementsUiState.Ready
        assertEquals("Karate", state.disciplineName)
    }

    @Test
    fun `disciplineId used as fallback name when discipline not found`() = runTest {
        val repo = FakeContentRepository(
            movementsByDiscipline = mapOf("karate" to listOf(movement("karate", "kick")))
        )
        val vm = MovementsViewModel(savedState("karate"), repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as MovementsUiState.Ready
        assertEquals("karate", state.disciplineName)
    }

    @Test
    fun `uiState is Error when repository throws`() = runTest {
        val repo = FakeContentRepository(throwOnMovements = RuntimeException("db error"))
        val vm = MovementsViewModel(savedState("karate"), repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertIs<MovementsUiState.Error>(vm.uiState.value)
    }

    @Test
    fun `toggleDifficulty filters movements by difficulty`() = runTest {
        val beginnerMovement = movement("karate", "front_kick")
            .copy(difficulty = Difficulty.BEGINNER)
        val advancedMovement = movement("karate", "spinning_heel_kick")
            .copy(difficulty = Difficulty.ADVANCED)
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(beginnerMovement, advancedMovement))
        )
        val vm = MovementsViewModel(savedState("karate"), repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        val stateAll = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(2, stateAll.movements.size)

        vm.toggleDifficulty(Difficulty.ADVANCED)
        advanceUntilIdle()
        val stateAdvanced = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(listOf(advancedMovement), stateAdvanced.movements)
        assertEquals(setOf(Difficulty.ADVANCED), stateAdvanced.selectedDifficulties)

        vm.toggleDifficulty(Difficulty.ADVANCED)
        advanceUntilIdle()
        val stateCleared = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(2, stateCleared.movements.size)
        assertEquals(emptySet(), stateCleared.selectedDifficulties)
    }

    // ── sort ──────────────────────────────────────────────────────────────

    @Test
    fun `default sort is BY_DIFFICULTY`() = runTest {
        val vm = MovementsViewModel(savedState("karate"), FakeContentRepository())
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(MovementsSort.BY_DIFFICULTY, state.sort)
    }

    @Test
    fun `toggleSort switches from BY_DIFFICULTY to BY_NAME`() = runTest {
        val vm = MovementsViewModel(savedState("karate"), FakeContentRepository())
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleSort()
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(MovementsSort.BY_NAME, state.sort)
    }

    @Test
    fun `toggleSort called twice returns to BY_DIFFICULTY`() = runTest {
        val vm = MovementsViewModel(savedState("karate"), FakeContentRepository())
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleSort()
        advanceUntilIdle()
        vm.toggleSort()
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(MovementsSort.BY_DIFFICULTY, state.sort)
    }

    @Test
    fun `BY_NAME sort produces alphabetical order`() = runTest {
        val zulu = movement("karate", "zanshin").copy(difficulty = Difficulty.ADVANCED)
        val alpha = movement("karate", "age_uke").copy(difficulty = Difficulty.BEGINNER)
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(zulu, alpha))
        )
        val vm = MovementsViewModel(savedState("karate"), repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleSort()
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals("karate.age_uke", state.movements[0].id)
        assertEquals("karate.zanshin", state.movements[1].id)
    }

    @Test
    fun `BY_DIFFICULTY sort groups beginner before intermediate before advanced`() = runTest {
        val advanced = movement("karate", "spinning_heel").copy(difficulty = Difficulty.ADVANCED)
        val beginner = movement("karate", "front_kick").copy(difficulty = Difficulty.BEGINNER)
        val intermediate = movement("karate", "roundhouse").copy(difficulty = Difficulty.INTERMEDIATE)
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(advanced, intermediate, beginner))
        )
        val vm = MovementsViewModel(savedState("karate"), repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(Difficulty.BEGINNER, state.movements[0].difficulty)
        assertEquals(Difficulty.INTERMEDIATE, state.movements[1].difficulty)
        assertEquals(Difficulty.ADVANCED, state.movements[2].difficulty)
    }

    @Test
    fun `BY_DIFFICULTY uses name as tiebreaker within same difficulty`() = runTest {
        val beta = movement("karate", "beta_kick").copy(difficulty = Difficulty.BEGINNER)
        val alpha = movement("karate", "alpha_kick").copy(difficulty = Difficulty.BEGINNER)
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(beta, alpha))
        )
        val vm = MovementsViewModel(savedState("karate"), repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals("karate.alpha_kick", state.movements[0].id)
        assertEquals("karate.beta_kick", state.movements[1].id)
    }

    // ── clearFilters ───────────────────────────────────────────────────────

    @Test
    fun `clearFilters resets selectedDifficulties and shows all movements`() = runTest {
        val beginner = movement("karate", "front_kick").copy(difficulty = Difficulty.BEGINNER)
        val advanced = movement("karate", "spinning_heel").copy(difficulty = Difficulty.ADVANCED)
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(beginner, advanced))
        )
        val vm = MovementsViewModel(savedState("karate"), repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.toggleDifficulty(Difficulty.ADVANCED)
        advanceUntilIdle()
        assertEquals(1, (vm.uiState.value as MovementsUiState.Ready).movements.size)

        vm.clearFilters()
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(2, state.movements.size)
        assertEquals(emptySet(), state.selectedDifficulties)
    }

    @Test
    fun `clearFilters resets selectedTags`() = runTest {
        val taggedMovement = movement("karate", "front_kick").copy(tags = listOf("kick"))
        val untagged = movement("karate", "arm_block").copy(tags = emptyList())
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(taggedMovement, untagged))
        )
        val vm = MovementsViewModel(savedState("karate"), repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.toggleTag("kick")
        advanceUntilIdle()
        assertEquals(1, (vm.uiState.value as MovementsUiState.Ready).movements.size)

        vm.clearFilters()
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(2, state.movements.size)
        assertEquals(emptySet(), state.selectedTags)
    }
}
