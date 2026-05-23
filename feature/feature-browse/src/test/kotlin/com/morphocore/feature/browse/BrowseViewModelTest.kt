package com.morphocore.feature.browse

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
import kotlin.test.assertTrue

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

    // ── difficultyBreakdown ───────────────────────────────────────────────

    @Test
    fun `difficultyBreakdown counts movements by difficulty`() = runTest {
        val repo = FakeContentRepository(
            movementsByDiscipline = mapOf(
                "karate" to listOf(
                    movement("karate", "front_kick").copy(difficulty = Difficulty.BEGINNER),
                    movement("karate", "roundhouse").copy(difficulty = Difficulty.INTERMEDIATE),
                    movement("karate", "spinning").copy(difficulty = Difficulty.ADVANCED),
                    movement("karate", "jab").copy(difficulty = Difficulty.BEGINNER)
                )
            )
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(2, state.difficultyBreakdown[Difficulty.BEGINNER])
        assertEquals(1, state.difficultyBreakdown[Difficulty.INTERMEDIATE])
        assertEquals(1, state.difficultyBreakdown[Difficulty.ADVANCED])
    }

    @Test
    fun `difficultyBreakdown is empty when no movements`() = runTest {
        val vm = vm()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(emptyMap<Difficulty, Int>(), state.difficultyBreakdown)
    }

    @Test
    fun `difficultyBreakdown not included in search mode`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf(
                "karate" to listOf(movement("karate", "front_kick"))
            )
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setQuery("kick")
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(emptyMap<Difficulty, Int>(), state.difficultyBreakdown)
    }

    // ── FakeContentRepository observeAllMovements fallback ────────────────

    @Test
    fun `totalMovementCount uses movementsByDiscipline when movementsById is empty`() = runTest {
        val repo = FakeContentRepository(
            movementsByDiscipline = mapOf(
                "karate" to listOf(
                    movement("karate", "front_kick"),
                    movement("karate", "roundhouse")
                ),
                "yoga" to listOf(movement("yoga", "downward_dog"))
            )
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(3, state.totalMovementCount)
    }

    // ── description-based search ──────────────────────────────────────────

    @Test
    fun `search matches disciplines by description`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(
                discipline("karate", "Karate").copy(description = "Japanese striking art"),
                discipline("yoga", "Yoga").copy(description = "Indian flexibility practice")
            )
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setQuery("striking")
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(1, state.disciplines.size)
        assertEquals("karate", state.disciplines.first().id)
    }

    @Test
    fun `search matches movements by description`() = runTest {
        val kickWithDesc = movement("karate", "mae_geri")
            .copy(description = "A powerful front kick using the ball of the foot")
        val punchWithDesc = movement("karate", "gyaku_zuki")
            .copy(description = "A strong reverse punch with hip rotation")
        val repo = FakeContentRepository(
            movementsById = mapOf(kickWithDesc.id to kickWithDesc, punchWithDesc.id to punchWithDesc)
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setQuery("hip rotation")
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(1, state.movementResults.size)
        assertEquals(punchWithDesc.id, state.movementResults.first().id)
    }

    // ── search result ordering ────────────────────────────────────────────

    @Test
    fun `movement search results are sorted alphabetically by name`() = runTest {
        val zulu = movement("gym", "squat").copy(name = "Squat")
        val alpha = movement("gym", "deadlift").copy(name = "Deadlift")
        val repo = FakeContentRepository(
            movementsById = mapOf(zulu.id to zulu, alpha.id to alpha)
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setQuery("a")
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        if (state.movementResults.size == 2) {
            assertEquals("Deadlift", state.movementResults[0].name)
            assertEquals("Squat", state.movementResults[1].name)
        }
    }

    // ── disciplineBreakdowns ──────────────────────────────────────────────

    @Test
    fun `disciplineBreakdowns maps discipline ids to their difficulty counts`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate"), discipline("gym", "Gym")),
            movementsById = mapOf(
                "karate.jab" to movement("karate", "jab").copy(difficulty = Difficulty.BEGINNER),
                "karate.roundhouse" to movement("karate", "roundhouse").copy(difficulty = Difficulty.INTERMEDIATE),
                "gym.squat" to movement("gym", "squat").copy(difficulty = Difficulty.ADVANCED)
            )
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(1, state.disciplineBreakdowns["karate"]?.get(Difficulty.BEGINNER))
        assertEquals(1, state.disciplineBreakdowns["karate"]?.get(Difficulty.INTERMEDIATE))
        assertEquals(1, state.disciplineBreakdowns["gym"]?.get(Difficulty.ADVANCED))
    }

    @Test
    fun `disciplineBreakdowns is empty in search mode`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsById = mapOf("karate.jab" to movement("karate", "jab"))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setQuery("jab")
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(emptyMap<String, Map<Difficulty, Int>>(), state.disciplineBreakdowns)
    }

    // ── difficulty filter ─────────────────────────────────────────────────

    @Test
    fun `toggleDifficultyFilter filters disciplines to those with matching difficulty`() = runTest {
        val beginnerMovement = movement("karate", "jab").copy(difficulty = Difficulty.BEGINNER)
        val advancedMovement = movement("gym", "planche").copy(difficulty = Difficulty.ADVANCED)
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate"), discipline("gym", "Gym")),
            movementsById = mapOf(beginnerMovement.id to beginnerMovement, advancedMovement.id to advancedMovement)
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleDifficultyFilter(Difficulty.BEGINNER)
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(1, state.disciplines.size)
        assertEquals("karate", state.disciplines.first().id)
        assertEquals(Difficulty.BEGINNER, state.selectedDifficulty)
    }

    @Test
    fun `toggleDifficultyFilter selecting same difficulty clears the filter`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsById = mapOf("karate.jab" to movement("karate", "jab").copy(difficulty = Difficulty.BEGINNER))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleDifficultyFilter(Difficulty.BEGINNER)
        advanceUntilIdle()
        vm.toggleDifficultyFilter(Difficulty.BEGINNER)
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(1, state.disciplines.size)
        assertEquals(null, state.selectedDifficulty)
    }

    @Test
    fun `selectedDifficulty is null by default`() = runTest {
        val vm = vm()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(null, state.selectedDifficulty)
    }

    @Test
    fun `toggleDifficultyFilter shows all disciplines when no movement has matching difficulty`() = runTest {
        val beginnerMovement = movement("karate", "jab").copy(difficulty = Difficulty.BEGINNER)
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate"), discipline("yoga", "Yoga")),
            movementsById = mapOf(beginnerMovement.id to beginnerMovement)
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleDifficultyFilter(Difficulty.ADVANCED)
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(0, state.disciplines.size)
    }

    @Test
    fun `clearDifficultyFilter resets selectedDifficulty to null`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsById = mapOf("karate.jab" to movement("karate", "jab").copy(difficulty = Difficulty.BEGINNER))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleDifficultyFilter(Difficulty.BEGINNER)
        advanceUntilIdle()
        assertEquals(Difficulty.BEGINNER, (vm.uiState.value as BrowseUiState.Ready).selectedDifficulty)
        vm.clearDifficultyFilter()
        advanceUntilIdle()
        assertEquals(null, (vm.uiState.value as BrowseUiState.Ready).selectedDifficulty)
    }

    // ── muscle group search ───────────────────────────────────────────────

    @Test
    fun `search matches movements by muscle group name`() = runTest {
        val shouldersMovement = movement("gym", "overhead_press")
            .copy(muscles = listOf(MuscleGroup.Shoulders))
        val legsMovement = movement("gym", "squat")
            .copy(muscles = listOf(MuscleGroup.Quadriceps))
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("gym", "Gym")),
            movementsById = mapOf(
                shouldersMovement.id to shouldersMovement,
                legsMovement.id to legsMovement
            )
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setQuery("shoulders")
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(1, state.movementResults.size)
        assertEquals(shouldersMovement.id, state.movementResults.first().id)
    }

    // ── disciplineMuscleBreakdowns ────────────────────────────────────────

    @Test
    fun `disciplineMuscleBreakdowns maps discipline ids to their muscle group counts`() = runTest {
        val m1 = movement("gym", "bench_press").copy(muscles = listOf(MuscleGroup.Chest, MuscleGroup.Shoulders))
        val m2 = movement("gym", "overhead_press").copy(muscles = listOf(MuscleGroup.Shoulders))
        val m3 = movement("karate", "punch").copy(muscles = listOf(MuscleGroup.Shoulders, MuscleGroup.Core))
        val repo = FakeContentRepository(
            movementsById = mapOf(m1.id to m1, m2.id to m2, m3.id to m3)
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(1, state.disciplineMuscleBreakdowns["gym"]?.get(MuscleGroup.Chest))
        assertEquals(2, state.disciplineMuscleBreakdowns["gym"]?.get(MuscleGroup.Shoulders))
        assertEquals(1, state.disciplineMuscleBreakdowns["karate"]?.get(MuscleGroup.Core))
    }

    // ── disciplineFilteredCounts ──────────────────────────────────────────

    @Test
    fun `disciplineFilteredCounts reflects intersection when both difficulty and muscle active`() = runTest {
        val beginnerChest = movement("gym", "bench_press")
            .copy(difficulty = Difficulty.BEGINNER, muscles = listOf(MuscleGroup.Chest))
        val advancedChest = movement("gym", "weighted_dip")
            .copy(difficulty = Difficulty.ADVANCED, muscles = listOf(MuscleGroup.Chest))
        val beginnerLegs = movement("gym", "squat")
            .copy(difficulty = Difficulty.BEGINNER, muscles = listOf(MuscleGroup.Quadriceps))
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("gym", "Gym")),
            movementsById = mapOf(
                beginnerChest.id to beginnerChest,
                advancedChest.id to advancedChest,
                beginnerLegs.id to beginnerLegs
            )
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleDifficultyFilter(Difficulty.BEGINNER)
        vm.toggleMuscleFilter(MuscleGroup.Chest)
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(1, state.disciplineFilteredCounts["gym"])
    }

    @Test
    fun `disciplineFilteredCounts is empty when no filter active`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("gym", "Gym")),
            movementsById = mapOf("gym.squat" to movement("gym", "squat"))
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(emptyMap<String, Int>(), state.disciplineFilteredCounts)
    }

    // ── muscle filter ─────────────────────────────────────────────────────

    @Test
    fun `toggleMuscleFilter filters disciplines to those with matching muscle movements`() = runTest {
        val chestMovement = movement("gym", "bench_press").copy(muscles = listOf(MuscleGroup.Chest))
        val coreMovement = movement("karate", "mae_geri").copy(muscles = listOf(MuscleGroup.Core))
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("gym", "Gym"), discipline("karate", "Karate")),
            movementsById = mapOf(chestMovement.id to chestMovement, coreMovement.id to coreMovement)
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleMuscleFilter(MuscleGroup.Chest)
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(1, state.disciplines.size)
        assertEquals("gym", state.disciplines.first().id)
        assertEquals(MuscleGroup.Chest, state.selectedMuscle)
    }

    @Test
    fun `toggleMuscleFilter selecting same muscle clears the filter`() = runTest {
        val chestMovement = movement("gym", "bench_press").copy(muscles = listOf(MuscleGroup.Chest))
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("gym", "Gym")),
            movementsById = mapOf(chestMovement.id to chestMovement)
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleMuscleFilter(MuscleGroup.Chest)
        advanceUntilIdle()
        vm.toggleMuscleFilter(MuscleGroup.Chest)
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(null, state.selectedMuscle)
    }

    @Test
    fun `clearFilters resets both difficulty and muscle selection`() = runTest {
        val m = movement("gym", "bench_press")
            .copy(muscles = listOf(MuscleGroup.Chest), difficulty = Difficulty.BEGINNER)
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("gym", "Gym")),
            movementsById = mapOf(m.id to m)
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleDifficultyFilter(Difficulty.BEGINNER)
        vm.toggleMuscleFilter(MuscleGroup.Chest)
        advanceUntilIdle()
        vm.clearFilters()
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(null, state.selectedDifficulty)
        assertEquals(null, state.selectedMuscle)
    }

    @Test
    fun `availableMuscles lists distinct muscle groups from all movements`() = runTest {
        val m1 = movement("gym", "squat").copy(muscles = listOf(MuscleGroup.Quadriceps, MuscleGroup.Glutes))
        val m2 = movement("karate", "punch").copy(muscles = listOf(MuscleGroup.Shoulders, MuscleGroup.Quadriceps))
        val repo = FakeContentRepository(
            movementsById = mapOf(m1.id to m1, m2.id to m2)
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(3, state.availableMuscles.size)
        assertTrue(MuscleGroup.Quadriceps in state.availableMuscles)
        assertTrue(MuscleGroup.Glutes in state.availableMuscles)
        assertTrue(MuscleGroup.Shoulders in state.availableMuscles)
    }

    @Test
    fun `discipline list is empty when active filter matches no movements`() = runTest {
        val beginnerMovement = movement("gym", "squat").copy(difficulty = Difficulty.BEGINNER)
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("gym", "Gym")),
            movementsById = mapOf(beginnerMovement.id to beginnerMovement)
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggleDifficultyFilter(Difficulty.ADVANCED)
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(emptyList<com.morphocore.domain.Discipline>(), state.disciplines)
        assertEquals(Difficulty.ADVANCED, state.selectedDifficulty)
    }

    @Test
    fun `search matches hip flexors using spaced token`() = runTest {
        val hipMovement = movement("karate", "mae_geri")
            .copy(muscles = listOf(MuscleGroup.HipFlexors))
        val repo = FakeContentRepository(
            movementsById = mapOf(hipMovement.id to hipMovement)
        )
        val vm = vm(repo = repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.setQuery("hip")
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(1, state.movementResults.size)
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
