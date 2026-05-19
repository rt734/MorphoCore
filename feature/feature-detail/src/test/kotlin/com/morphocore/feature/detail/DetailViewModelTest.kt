package com.morphocore.feature.detail

import androidx.lifecycle.SavedStateHandle
import com.morphocore.content.api.ContentRepository
import com.morphocore.content.testing.FakeContentRepository
import com.morphocore.preferences.api.UserPreferences
import com.morphocore.domain.AnimationClip
import com.morphocore.domain.Difficulty
import com.morphocore.domain.DirectLightConfig
import com.morphocore.domain.Discipline
import com.morphocore.domain.GroundPlaneConfig
import com.morphocore.domain.MorphoColors
import com.morphocore.domain.MorphoMotion
import com.morphocore.domain.MorphoShapes
import com.morphocore.domain.MorphoTypography
import com.morphocore.domain.Movement
import com.morphocore.domain.PostProcessingConfig
import com.morphocore.domain.SceneConfig
import com.morphocore.domain.Theme
import com.morphocore.theme.api.ThemeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
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
class DetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeUserPreferences : UserPreferences {
        override fun getDefaultSpeed(): Float = 1f
        override fun setDefaultSpeed(speed: Float) {}
        override fun getDefaultCamera(): String? = null
        override fun setDefaultCamera(preset: String?) {}
    }

    private class FakeThemeProvider(theme: Theme) : ThemeProvider {
        private val _activeTheme = MutableStateFlow(theme)
        override val activeTheme: StateFlow<Theme> = _activeTheme.asStateFlow()
        override suspend fun setActiveTheme(themeId: String) {}
    }

    private fun fakeTheme() = Theme(
        id = "default", name = "Default", description = "", isDefault = true,
        colors = MorphoColors(
            0xFF000000L, 0xFFFFFFFFL, 0xFF000000L, 0xFFFFFFFFL,
            0xFF000000L, 0xFFFFFFFFL, 0xFF000000L, 0xFFFFFFFFL,
            0xFF000000L, 0xFFFFFFFFL, 0xFF000000L, emptyMap()
        ),
        typography = MorphoTypography(null, null, null, emptyMap()),
        shapes = MorphoShapes(4f, 8f, 16f),
        motion = MorphoMotion(150, 300, 500, "standard"),
        scene = SceneConfig(
            skyboxPath = null,
            iblEnvironmentPath = "envs/default.hdr",
            iblIntensity = 30000f,
            directLight = DirectLightConfig(0xFFFFFFFFL, 100000f, 0f, 45f),
            ambientIntensity = 10000f,
            groundPlane = GroundPlaneConfig(true, 0xFF888888L, 0.5f),
            postProcessing = PostProcessingConfig(0f, 0f, "ACES")
        )
    )

    private fun fakeMovement(id: String = "karate.mae-geri") = Movement(
        id = id, disciplineId = "karate", name = "Mae Geri",
        modelPath = "models/mae_geri.glb", defaultClip = "idle",
        clips = listOf(AnimationClip("idle", 1.0f, 30)),
        muscles = emptyList(), difficulty = Difficulty.BEGINNER,
        tags = emptyList(), cameraPreset = null, prerequisites = emptyList(), commonMistakes = emptyList()
    )

    private fun vm(movementId: String, repo: ContentRepository = FakeContentRepository()) =
        DetailViewModel(
            SavedStateHandle(mapOf("movementId" to movementId)),
            repo,
            FakeThemeProvider(fakeTheme()),
            FakeUserPreferences()
        )

    @Test
    fun `uiState is Loading initially`() = runTest {
        val vm = vm("karate.mae-geri")
        assertIs<DetailUiState.Loading>(vm.uiState.value)
    }

    @Test
    fun `uiState is Ready when movement found`() = runTest {
        val movement = fakeMovement()
        val repo = FakeContentRepository(movementsById = mapOf(movement.id to movement))
        val vm = vm(movement.id, repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<DetailUiState.Ready>(vm.uiState.value)
        assertEquals(movement.id, state.movement.id)
    }

    @Test
    fun `uiState is Error when movement not found`() = runTest {
        val vm = vm("karate.not-found")
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertIs<DetailUiState.Error>(vm.uiState.value)
    }

    @Test
    fun `uiState is Error when repository throws`() = runTest {
        val throwingRepo = object : ContentRepository {
            override fun observeDisciplines(): Flow<List<Discipline>> = flowOf(emptyList())
            override fun observeMovements(disciplineId: String): Flow<List<Movement>> = flowOf(emptyList())
            override suspend fun getMovement(movementId: String): Movement? =
                throw RuntimeException("network error")
        }
        val vm = vm("karate.any", throwingRepo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<DetailUiState.Error>(vm.uiState.value)
        assertEquals("network error", state.message)
    }

    @Test
    fun `togglePlayPause flips isPlaying`() = runTest {
        val movement = fakeMovement()
        val repo = FakeContentRepository(movementsById = mapOf(movement.id to movement))
        val vm = vm(movement.id, repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.onModelLoaded("idle")
        assertEquals(true, vm.playbackState.value.isPlaying)
        vm.togglePlayPause()
        assertEquals(false, vm.playbackState.value.isPlaying)
    }
}
