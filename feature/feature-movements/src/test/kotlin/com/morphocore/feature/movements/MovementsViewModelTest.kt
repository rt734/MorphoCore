package com.morphocore.feature.movements

import androidx.lifecycle.SavedStateHandle
import com.morphocore.content.api.ContentError
import com.morphocore.content.api.ContentRegistry
import com.morphocore.content.api.RegistryState
import com.morphocore.content.testing.FakeContentRepository
import com.morphocore.domain.Difficulty
import com.morphocore.domain.Discipline
import com.morphocore.domain.Movement
import com.morphocore.domain.MuscleGroup
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

    private fun vm(
        disciplineId: String = "karate",
        repo: FakeContentRepository = FakeContentRepository(),
        registry: FakeContentRegistry = FakeContentRegistry()
    ) = MovementsViewModel(savedState(disciplineId), repo, registry)

    // ── baseline ──────────────────────────────────────────────────────────

    @Test
    fun `uiState is Ready with empty movements when repository has no movements`() = runTest {
        val vm = vm()
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
        val vm = vm(repo = repo)
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
        val vm = vm(repo = repo)
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
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as MovementsUiState.Ready
        assertEquals("karate", state.disciplineName)
    }

    @Test
    fun `uiState is Error when repository throws`() = runTest {
        val repo = FakeContentRepository(throwOnMovements = RuntimeException("db error"))
        val vm = vm(repo = repo)
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
        val vm = vm(repo = repo)
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
        val vm = vm()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(MovementsSort.BY_DIFFICULTY, state.sort)
    }

    @Test
    fun `toggleSort switches from BY_DIFFICULTY to BY_NAME`() = runTest {
        val vm = vm()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleSort()
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(MovementsSort.BY_NAME, state.sort)
    }

    @Test
    fun `toggleSort called twice returns to BY_DIFFICULTY`() = runTest {
        val vm = vm()
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
        val vm = vm(repo = repo)
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
        val vm = vm(repo = repo)
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
        val vm = vm(repo = repo)
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
        val vm = vm(repo = repo)
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

    // ── disciplineDescription ─────────────────────────────────────────────

    @Test
    fun `disciplineDescription is populated from discipline`() = runTest {
        val disciplineWithDesc = Discipline(
            id = "karate", name = "Karate",
            description = "Traditional Japanese martial art",
            iconPath = null, movementIds = emptyList()
        )
        val repo = FakeContentRepository(
            disciplines = listOf(disciplineWithDesc),
            movementsByDiscipline = mapOf("karate" to listOf(movement("karate", "front_kick")))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals("Traditional Japanese martial art", state.disciplineDescription)
    }

    @Test
    fun `disciplineDescription is empty string when discipline not found`() = runTest {
        val repo = FakeContentRepository(
            movementsByDiscipline = mapOf("karate" to listOf(movement("karate", "kick")))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals("", state.disciplineDescription)
    }

    // ── difficultyBreakdown ───────────────────────────────────────────────

    @Test
    fun `difficultyBreakdown counts movements by difficulty`() = runTest {
        val beginner1 = movement("karate", "front_kick").copy(difficulty = Difficulty.BEGINNER)
        val beginner2 = movement("karate", "arm_block").copy(difficulty = Difficulty.BEGINNER)
        val advanced = movement("karate", "spinning_heel").copy(difficulty = Difficulty.ADVANCED)
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(beginner1, beginner2, advanced))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(2, state.difficultyBreakdown[Difficulty.BEGINNER])
        assertEquals(null, state.difficultyBreakdown[Difficulty.INTERMEDIATE])
        assertEquals(1, state.difficultyBreakdown[Difficulty.ADVANCED])
    }

    @Test
    fun `difficultyBreakdown reflects unfiltered movements even when filter active`() = runTest {
        val beginner = movement("karate", "front_kick").copy(difficulty = Difficulty.BEGINNER)
        val advanced = movement("karate", "spinning_heel").copy(difficulty = Difficulty.ADVANCED)
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(beginner, advanced))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleDifficulty(Difficulty.ADVANCED)
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        // movements list is filtered (1), but breakdown still reflects full set (2)
        assertEquals(1, state.movements.size)
        assertEquals(1, state.difficultyBreakdown[Difficulty.BEGINNER])
        assertEquals(1, state.difficultyBreakdown[Difficulty.ADVANCED])
    }

    // ── tagCounts ─────────────────────────────────────────────────────────

    @Test
    fun `tagCounts reflects tag frequency across all unfiltered movements`() = runTest {
        val m1 = movement("karate", "front_kick").copy(tags = listOf("kick", "fundamental"))
        val m2 = movement("karate", "roundhouse").copy(tags = listOf("kick", "advanced-kick"))
        val m3 = movement("karate", "arm_block").copy(tags = listOf("block", "fundamental"))
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(m1, m2, m3))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(2, state.tagCounts["kick"])
        assertEquals(2, state.tagCounts["fundamental"])
        assertEquals(1, state.tagCounts["advanced-kick"])
        assertEquals(1, state.tagCounts["block"])
    }

    // ── totalCount ────────────────────────────────────────────────────────

    @Test
    fun `totalCount reflects all movements before filtering`() = runTest {
        val beginner = movement("karate", "front_kick").copy(difficulty = Difficulty.BEGINNER)
        val advanced = movement("karate", "spinning_heel").copy(difficulty = Difficulty.ADVANCED)
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(beginner, advanced))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(2, state.totalCount)
    }

    @Test
    fun `totalCount unchanged when filter reduces visible movements`() = runTest {
        val beginner = movement("karate", "front_kick").copy(difficulty = Difficulty.BEGINNER)
        val advanced = movement("karate", "spinning_heel").copy(difficulty = Difficulty.ADVANCED)
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(beginner, advanced))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleDifficulty(Difficulty.ADVANCED)
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(1, state.movements.size)
        assertEquals(2, state.totalCount)
    }

    // ── text search ───────────────────────────────────────────────────────

    @Test
    fun `setQuery filters movements by name`() = runTest {
        val frontKick = movement("karate", "front_kick")
        val armBlock = movement("karate", "arm_block")
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(frontKick, armBlock))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setQuery("front")
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(1, state.movements.size)
        assertEquals(frontKick.id, state.movements.first().id)
    }

    @Test
    fun `setQuery filters movements by tag`() = runTest {
        val kickMovement = movement("karate", "mae_geri").copy(tags = listOf("kick"))
        val blockMovement = movement("karate", "jodan_uke").copy(tags = listOf("block"))
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(kickMovement, blockMovement))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setQuery("kick")
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(1, state.movements.size)
        assertEquals(kickMovement.id, state.movements.first().id)
    }

    @Test
    fun `setQuery filters movements by description`() = runTest {
        val kickWithDesc = movement("karate", "mae_geri")
            .copy(description = "A front kick using the ball of the foot")
        val punchWithDesc = movement("karate", "gyaku_zuki")
            .copy(description = "A reverse punch with hip rotation")
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(kickWithDesc, punchWithDesc))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setQuery("hip rotation")
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(1, state.movements.size)
        assertEquals(punchWithDesc.id, state.movements.first().id)
    }

    @Test
    fun `setQuery filters movements by muscle group`() = runTest {
        val shouldersMovement = movement("gym", "overhead_press")
            .copy(muscles = listOf(MuscleGroup.Shoulders))
        val legsMovement = movement("gym", "squat")
            .copy(muscles = listOf(MuscleGroup.Quadriceps))
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("gym", "Gym")),
            movementsByDiscipline = mapOf("gym" to listOf(shouldersMovement, legsMovement))
        )
        val vm = vm(disciplineId = "gym", repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setQuery("shoulders")
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(1, state.movements.size)
        assertEquals(shouldersMovement.id, state.movements.first().id)
    }

    @Test
    fun `blank query shows all movements`() = runTest {
        val m1 = movement("karate", "front_kick")
        val m2 = movement("karate", "arm_block")
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(m1, m2))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setQuery("front")
        advanceUntilIdle()
        vm.setQuery("")
        advanceUntilIdle()
        val state = assertIs<MovementsUiState.Ready>(vm.uiState.value)
        assertEquals(2, state.movements.size)
    }

    @Test
    fun `clearFilters resets selectedTags`() = runTest {
        val taggedMovement = movement("karate", "front_kick").copy(tags = listOf("kick"))
        val untagged = movement("karate", "arm_block").copy(tags = emptyList())
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(taggedMovement, untagged))
        )
        val vm = vm(repo = repo)
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

    // ── registry error ────────────────────────────────────────────────────

    @Test
    fun `uiState is Error when registry emits error state`() = runTest {
        val registry = FakeContentRegistry(initialState = RegistryState.Loading)
        val vm = vm(registry = registry)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        registry.setState(RegistryState.Error(ContentError.NoContentFound))
        advanceUntilIdle()
        assertIs<MovementsUiState.Error>(vm.uiState.value)
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
