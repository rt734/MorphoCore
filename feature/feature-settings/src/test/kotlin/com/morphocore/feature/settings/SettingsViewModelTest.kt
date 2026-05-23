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
import com.morphocore.preferences.api.UserPreferences
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

    private fun vm(
        provider: FakeThemeProvider = FakeThemeProvider(fakeTheme("light")),
        registry: FakeThemeRegistry = FakeThemeRegistry(listOf(fakeTheme("light"))),
        prefs: FakeUserPreferences = FakeUserPreferences()
    ) = SettingsViewModel(provider, registry, prefs)

    // ── uiState ───────────────────────────────────────────────────────────

    @Test
    fun `uiState is Loading initially`() = runTest {
        val vm = vm()
        assertIs<SettingsUiState.Loading>(vm.uiState.value)
    }

    @Test
    fun `uiState is Ready with active theme`() = runTest {
        val vm = vm()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertIs<SettingsUiState.Ready>(vm.uiState.value)
    }

    @Test
    fun `Ready contains all themes from registry`() = runTest {
        val themes = listOf(fakeTheme("light"), fakeTheme("dark"))
        val vm = vm(registry = FakeThemeRegistry(themes))
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<SettingsUiState.Ready>(vm.uiState.value)
        assertEquals(2, state.themes.size)
    }

    @Test
    fun `selectTheme updates active theme in provider`() = runTest {
        val provider = FakeThemeProvider(fakeTheme("light"))
        val vm = vm(
            provider = provider,
            registry = FakeThemeRegistry(listOf(fakeTheme("light"), fakeTheme("dark")))
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
        val vm = vm(
            provider = FakeThemeProvider(fakeTheme("light")),
            registry = FakeThemeRegistry(listOf(fakeTheme("light"), fakeTheme("dark")))
        )
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<SettingsUiState.Ready>(vm.uiState.value)
        assertEquals("light", state.activeThemeId)
    }

    // ── defaultSpeed ──────────────────────────────────────────────────────

    @Test
    fun `defaultSpeed initial value comes from UserPreferences`() = runTest {
        val prefs = FakeUserPreferences(speed = 0.5f)
        val vm = vm(prefs = prefs)
        assertEquals(0.5f, vm.defaultSpeed.value)
    }

    @Test
    fun `setDefaultSpeed updates defaultSpeed state`() = runTest {
        val vm = vm()
        vm.setDefaultSpeed(1.5f)
        assertEquals(1.5f, vm.defaultSpeed.value)
    }

    @Test
    fun `setDefaultSpeed persists value to UserPreferences`() = runTest {
        val prefs = FakeUserPreferences()
        val vm = vm(prefs = prefs)
        vm.setDefaultSpeed(1.5f)
        assertEquals(1.5f, prefs.getDefaultSpeed())
    }

    // ── defaultCamera ─────────────────────────────────────────────────────

    @Test
    fun `defaultCamera initial value comes from UserPreferences`() = runTest {
        val prefs = FakeUserPreferences(camera = "side")
        val vm = vm(prefs = prefs)
        assertEquals("side", vm.defaultCamera.value)
    }

    @Test
    fun `setDefaultCamera updates defaultCamera state`() = runTest {
        val vm = vm()
        vm.setDefaultCamera("top")
        assertEquals("top", vm.defaultCamera.value)
    }

    @Test
    fun `setDefaultCamera persists value to UserPreferences`() = runTest {
        val prefs = FakeUserPreferences()
        val vm = vm(prefs = prefs)
        vm.setDefaultCamera("front")
        assertEquals("front", prefs.getDefaultCamera())
    }

    @Test
    fun `setDefaultCamera accepts null to clear preference`() = runTest {
        val prefs = FakeUserPreferences(camera = "side")
        val vm = vm(prefs = prefs)
        vm.setDefaultCamera(null)
        assertEquals(null, vm.defaultCamera.value)
        assertEquals(null, prefs.getDefaultCamera())
    }

    // ── resetPlaybackDefaults ─────────────────────────────────────────────

    @Test
    fun `resetPlaybackDefaults resets speed to 1f`() = runTest {
        val prefs = FakeUserPreferences(speed = 2f)
        val vm = vm(prefs = prefs)
        vm.resetPlaybackDefaults()
        assertEquals(1f, vm.defaultSpeed.value)
        assertEquals(1f, prefs.getDefaultSpeed())
    }

    @Test
    fun `resetPlaybackDefaults clears camera to null`() = runTest {
        val prefs = FakeUserPreferences(camera = "side")
        val vm = vm(prefs = prefs)
        vm.resetPlaybackDefaults()
        assertEquals(null, vm.defaultCamera.value)
        assertEquals(null, prefs.getDefaultCamera())
    }

    // ── uiState reactive ──────────────────────────────────────────────────

    @Test
    fun `uiState updates when active theme changes in provider`() = runTest {
        val provider = FakeThemeProvider(fakeTheme("light"))
        val registry = FakeThemeRegistry(listOf(fakeTheme("light"), fakeTheme("dark")))
        val vm = vm(provider = provider, registry = registry)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        provider._activeTheme.value = fakeTheme("dark")
        advanceUntilIdle()
        val state = assertIs<SettingsUiState.Ready>(vm.uiState.value)
        assertEquals("dark", state.activeThemeId)
    }

    @Test
    fun `uiState updates when registry themes list changes`() = runTest {
        val registry = FakeThemeRegistry(listOf(fakeTheme("light")))
        val vm = vm(registry = registry)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        registry._themes.value = listOf(fakeTheme("light"), fakeTheme("dark"), fakeTheme("sepia"))
        advanceUntilIdle()
        val state = assertIs<SettingsUiState.Ready>(vm.uiState.value)
        assertEquals(3, state.themes.size)
    }

    @Test
    fun `uiState Ready themes preserves registry order`() = runTest {
        val ordered = listOf(fakeTheme("a"), fakeTheme("b"), fakeTheme("c"))
        val vm = vm(registry = FakeThemeRegistry(ordered))
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<SettingsUiState.Ready>(vm.uiState.value)
        assertEquals(listOf("a", "b", "c"), state.themes.map { it.id })
    }

    @Test
    fun `uiState Ready with empty themes list`() = runTest {
        val vm = vm(registry = FakeThemeRegistry(emptyList()))
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<SettingsUiState.Ready>(vm.uiState.value)
        assertEquals(0, state.themes.size)
    }

    // ── selectTheme edge cases ────────────────────────────────────────────

    @Test
    fun `selectTheme triggers uiState Ready update`() = runTest {
        val provider = FakeThemeProvider(fakeTheme("light"))
        val registry = FakeThemeRegistry(listOf(fakeTheme("light"), fakeTheme("dark")))
        val vm = vm(provider = provider, registry = registry)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.selectTheme("dark")
        advanceUntilIdle()
        assertIs<SettingsUiState.Ready>(vm.uiState.value)
    }

    @Test
    fun `multiple consecutive selectTheme calls reflect latest selection`() = runTest {
        val provider = FakeThemeProvider(fakeTheme("light"))
        val registry = FakeThemeRegistry(listOf(fakeTheme("light"), fakeTheme("dark"), fakeTheme("sepia")))
        val vm = vm(provider = provider, registry = registry)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.selectTheme("dark")
        vm.selectTheme("sepia")
        advanceUntilIdle()
        val state = assertIs<SettingsUiState.Ready>(vm.uiState.value)
        assertEquals("sepia", state.activeThemeId)
    }

    // ── defaultSpeed boundary ─────────────────────────────────────────────

    @Test
    fun `setDefaultSpeed to zero is accepted`() = runTest {
        val prefs = FakeUserPreferences()
        val vm = vm(prefs = prefs)
        vm.setDefaultSpeed(0f)
        assertEquals(0f, vm.defaultSpeed.value)
        assertEquals(0f, prefs.getDefaultSpeed())
    }

    @Test
    fun `setDefaultSpeed below 1 is accepted`() = runTest {
        val prefs = FakeUserPreferences()
        val vm = vm(prefs = prefs)
        vm.setDefaultSpeed(0.25f)
        assertEquals(0.25f, vm.defaultSpeed.value)
        assertEquals(0.25f, prefs.getDefaultSpeed())
    }

    @Test
    fun `setDefaultSpeed multiple times reflects last value`() = runTest {
        val vm = vm()
        vm.setDefaultSpeed(0.5f)
        vm.setDefaultSpeed(1.5f)
        vm.setDefaultSpeed(2f)
        assertEquals(2f, vm.defaultSpeed.value)
    }

    // ── defaultCamera extended ────────────────────────────────────────────

    @Test
    fun `defaultCamera is null when prefs has no camera set`() = runTest {
        val vm = vm(prefs = FakeUserPreferences(camera = null))
        assertEquals(null, vm.defaultCamera.value)
    }

    @Test
    fun `setDefaultCamera multiple times reflects last value`() = runTest {
        val vm = vm()
        vm.setDefaultCamera("front")
        vm.setDefaultCamera("side")
        vm.setDefaultCamera("top")
        assertEquals("top", vm.defaultCamera.value)
    }

    @Test
    fun `setDefaultCamera with three_quarter preset`() = runTest {
        val prefs = FakeUserPreferences()
        val vm = vm(prefs = prefs)
        vm.setDefaultCamera("three_quarter")
        assertEquals("three_quarter", vm.defaultCamera.value)
        assertEquals("three_quarter", prefs.getDefaultCamera())
    }

    @Test
    fun `setDefaultCamera with front preset`() = runTest {
        val prefs = FakeUserPreferences()
        val vm = vm(prefs = prefs)
        vm.setDefaultCamera("front")
        assertEquals("front", vm.defaultCamera.value)
        assertEquals("front", prefs.getDefaultCamera())
    }

    // ── resetPlaybackDefaults edge cases ──────────────────────────────────

    @Test
    fun `resetPlaybackDefaults when already at defaults is idempotent`() = runTest {
        val prefs = FakeUserPreferences(speed = 1f, camera = null)
        val vm = vm(prefs = prefs)
        vm.resetPlaybackDefaults()
        assertEquals(1f, vm.defaultSpeed.value)
        assertEquals(null, vm.defaultCamera.value)
    }

    @Test
    fun `resetPlaybackDefaults does not affect theme selection`() = runTest {
        val provider = FakeThemeProvider(fakeTheme("dark"))
        val registry = FakeThemeRegistry(listOf(fakeTheme("light"), fakeTheme("dark")))
        val prefs = FakeUserPreferences(speed = 2f, camera = "side")
        val vm = vm(provider = provider, registry = registry, prefs = prefs)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.resetPlaybackDefaults()
        advanceUntilIdle()
        val state = assertIs<SettingsUiState.Ready>(vm.uiState.value)
        assertEquals("dark", state.activeThemeId)
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

private class FakeUserPreferences(
    private var speed: Float = 1f,
    private var camera: String? = null
) : UserPreferences {
    override fun getDefaultSpeed(): Float = speed
    override fun setDefaultSpeed(s: Float) { speed = s }
    override fun getDefaultCamera(): String? = camera
    override fun setDefaultCamera(p: String?) { camera = p }
}
