package com.morphocore.feature.settings

import com.morphocore.domain.DirectLightConfig
import com.morphocore.domain.GroundPlaneConfig
import com.morphocore.domain.MorphoColors
import com.morphocore.domain.MorphoMotion
import com.morphocore.domain.MorphoShapes
import com.morphocore.domain.MorphoTypography
import com.morphocore.domain.PostProcessingConfig
import com.morphocore.domain.SceneConfig
import com.morphocore.domain.Theme
import com.morphocore.theme.api.ThemeProvider
import com.morphocore.theme.api.ThemeRegistry
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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fakeTheme(id: String, name: String = id, isDefault: Boolean = false) = Theme(
        id = id,
        name = name,
        description = "Description for $name",
        isDefault = isDefault,
        colors = MorphoColors(
            primary = 0xFF000000L,
            onPrimary = 0xFFFFFFFFL,
            secondary = 0xFF000000L,
            onSecondary = 0xFFFFFFFFL,
            background = 0xFF000000L,
            onBackground = 0xFFFFFFFFL,
            surface = 0xFF000000L,
            onSurface = 0xFFFFFFFFL,
            surfaceVariant = 0xFF000000L,
            onSurfaceVariant = 0xFFFFFFFFL,
            outline = 0xFF000000L,
            semantic = emptyMap()
        ),
        typography = MorphoTypography(
            displayFontPath = null,
            bodyFontPath = null,
            labelFontPath = null,
            scale = emptyMap()
        ),
        shapes = MorphoShapes(smallDp = 4f, mediumDp = 8f, largeDp = 16f),
        motion = MorphoMotion(
            durationShortMs = 150,
            durationMediumMs = 300,
            durationLongMs = 500,
            easingStandard = "standard"
        ),
        scene = SceneConfig(
            skyboxPath = null,
            iblEnvironmentPath = "envs/default.hdr",
            iblIntensity = 30000f,
            directLight = DirectLightConfig(
                color = 0xFFFFFFFFL,
                intensityLux = 100000f,
                azimuthDegrees = 0f,
                elevationDegrees = 45f
            ),
            ambientIntensity = 10000f,
            groundPlane = GroundPlaneConfig(enabled = true, color = 0xFF888888L, opacity = 0.5f),
            postProcessing = PostProcessingConfig(
                bloomIntensity = 0f,
                vignetteIntensity = 0f,
                toneMapping = "ACES"
            )
        )
    )

    @Test
    fun `uiState is Loading initially`() = runTest {
        val vm = SettingsViewModel(
            themeProvider = FakeThemeProvider(fakeTheme("light")),
            themeRegistry = FakeThemeRegistry(listOf(fakeTheme("light")))
        )
        assertIs<SettingsUiState.Loading>(vm.uiState.value)
    }

    @Test
    fun `uiState is Ready with active theme`() = runTest {
        val vm = SettingsViewModel(
            themeProvider = FakeThemeProvider(fakeTheme("light")),
            themeRegistry = FakeThemeRegistry(listOf(fakeTheme("light")))
        )
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertIs<SettingsUiState.Ready>(vm.uiState.value)
    }

    @Test
    fun `Ready contains all themes from registry`() = runTest {
        val themes = listOf(fakeTheme("light"), fakeTheme("dark"))
        val vm = SettingsViewModel(
            themeProvider = FakeThemeProvider(fakeTheme("light")),
            themeRegistry = FakeThemeRegistry(themes)
        )
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<SettingsUiState.Ready>(vm.uiState.value)
        assertEquals(2, state.themes.size)
    }

    @Test
    fun `selectTheme updates active theme in provider`() = runTest {
        val provider = FakeThemeProvider(fakeTheme("light"))
        val vm = SettingsViewModel(
            themeProvider = provider,
            themeRegistry = FakeThemeRegistry(listOf(fakeTheme("light"), fakeTheme("dark")))
        )
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.selectTheme("dark")
        advanceUntilIdle()
        val state = assertIs<SettingsUiState.Ready>(vm.uiState.value)
        assertEquals("dark", state.activeThemeId)
    }

    @Test
    fun `activeThemeId reflects current active theme`() = runTest {
        val activeTheme = fakeTheme("light")
        val vm = SettingsViewModel(
            themeProvider = FakeThemeProvider(activeTheme),
            themeRegistry = FakeThemeRegistry(listOf(activeTheme, fakeTheme("dark")))
        )
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<SettingsUiState.Ready>(vm.uiState.value)
        assertEquals("light", state.activeThemeId)
    }
}

private class FakeThemeProvider(theme: Theme) : ThemeProvider {
    val _activeTheme = MutableStateFlow(theme)
    override val activeTheme: StateFlow<Theme> = _activeTheme.asStateFlow()
    override suspend fun setActiveTheme(themeId: String) {
        _activeTheme.value = _activeTheme.value.copy(id = themeId)
    }
}

private class FakeThemeRegistry(themes: List<Theme> = emptyList()) : ThemeRegistry {
    val _themes = MutableStateFlow(themes)
    override val themes: StateFlow<List<Theme>> = _themes.asStateFlow()
    override suspend fun refresh() {}
}
