package com.morphocore.theme.impl.registry

import com.morphocore.theme.impl.FakeThemeAssetSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeRegistryImplTest {

    private fun fixture(name: String) =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")).bufferedReader().readText()

    private val studioJson = fixture("studio-theme.json")
    private val dojoJson   = fixture("dojo-theme.json")

    @Test
    fun `themes is empty before refresh`() = runTest {
        val registry = ThemeRegistryImpl(
            source = FakeThemeAssetSource(manifests = mapOf("studio" to studioJson)),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            scope = this
        )
        assertTrue(registry.themes.value.isEmpty())
    }

    @Test
    fun `themes contains parsed themes after refresh`() = runTest {
        val registry = ThemeRegistryImpl(
            source = FakeThemeAssetSource(manifests = mapOf("studio" to studioJson)),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            scope = this
        )
        registry.refresh()
        advanceUntilIdle()
        assertEquals(1, registry.themes.value.size)
        assertEquals("studio", registry.themes.value.first().id)
    }

    @Test
    fun `multiple themes all loaded`() = runTest {
        val registry = ThemeRegistryImpl(
            source = FakeThemeAssetSource(manifests = mapOf("studio" to studioJson, "dojo" to dojoJson)),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            scope = this
        )
        registry.refresh()
        advanceUntilIdle()
        assertEquals(2, registry.themes.value.size)
    }

    @Test
    fun `one bad manifest does not block other themes`() = runTest {
        val registry = ThemeRegistryImpl(
            source = FakeThemeAssetSource(manifests = mapOf("studio" to studioJson, "bad" to "{ invalid }")),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            scope = this
        )
        registry.refresh()
        advanceUntilIdle()
        assertEquals(1, registry.themes.value.size)
        assertEquals("studio", registry.themes.value.first().id)
    }

    @Test
    fun `empty source leaves themes empty`() = runTest {
        val registry = ThemeRegistryImpl(
            source = FakeThemeAssetSource(manifests = emptyMap()),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            scope = this
        )
        registry.refresh()
        advanceUntilIdle()
        assertTrue(registry.themes.value.isEmpty())
    }
}
