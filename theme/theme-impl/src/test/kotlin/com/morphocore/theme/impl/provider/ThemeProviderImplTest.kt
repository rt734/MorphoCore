package com.morphocore.theme.impl.provider

import com.morphocore.theme.impl.FakeThemeAssetSource
import com.morphocore.theme.impl.registry.ThemeRegistryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeProviderImplTest {

    private fun fixture(name: String) =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")).bufferedReader().readText()

    private val studioJson = fixture("studio-theme.json")
    private val dojoJson   = fixture("dojo-theme.json")

    private suspend fun buildProvider(
        manifests: Map<String, String>,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher,
        scope: kotlinx.coroutines.CoroutineScope,
        savedId: String? = null
    ): ThemeProviderImpl {
        val registry = ThemeRegistryImpl(
            source = FakeThemeAssetSource(manifests = manifests),
            ioDispatcher = dispatcher,
            scope = scope
        )
        registry.refresh()
        val prefs = FakeThemePreferences().apply { lastId = savedId }
        return ThemeProviderImpl(registry = registry, prefs = prefs)
    }

    @Test
    fun `activates isDefault theme on first launch`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        advanceUntilIdle()
        val provider = buildProvider(
            manifests = mapOf("studio" to studioJson, "dojo" to dojoJson),
            dispatcher = dispatcher,
            scope = this
        )
        assertEquals("studio", provider.activeTheme.value.id)
    }

    @Test
    fun `restores previously saved theme id`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        advanceUntilIdle()
        val provider = buildProvider(
            manifests = mapOf("studio" to studioJson, "dojo" to dojoJson),
            dispatcher = dispatcher,
            scope = this,
            savedId = "dojo"
        )
        assertEquals("dojo", provider.activeTheme.value.id)
    }

    @Test
    fun `setActiveTheme updates flow`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        advanceUntilIdle()
        val provider = buildProvider(
            manifests = mapOf("studio" to studioJson, "dojo" to dojoJson),
            dispatcher = dispatcher,
            scope = this
        )
        provider.setActiveTheme("dojo")
        assertEquals("dojo", provider.activeTheme.value.id)
    }

    @Test
    fun `setActiveTheme persists id`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        advanceUntilIdle()
        val prefs = FakeThemePreferences()
        val registry = ThemeRegistryImpl(
            source = FakeThemeAssetSource(manifests = mapOf("studio" to studioJson, "dojo" to dojoJson)),
            ioDispatcher = dispatcher,
            scope = this
        )
        registry.refresh()
        val provider = ThemeProviderImpl(registry = registry, prefs = prefs)
        provider.setActiveTheme("dojo")
        assertEquals("dojo", prefs.lastId)
    }

    @Test
    fun `setActiveTheme with unknown id is a no-op`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        advanceUntilIdle()
        val provider = buildProvider(
            manifests = mapOf("studio" to studioJson),
            dispatcher = dispatcher,
            scope = this
        )
        val before = provider.activeTheme.value.id
        provider.setActiveTheme("nonexistent")
        assertEquals(before, provider.activeTheme.value.id)
    }

    @Test
    fun `falls back to first theme when no default and no saved id`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val ironJson = fixture("iron-theme.json")
        advanceUntilIdle()
        val provider = buildProvider(
            manifests = mapOf("dojo" to dojoJson, "iron" to ironJson),
            dispatcher = dispatcher,
            scope = this
        )
        assertNotNull(provider.activeTheme.value)
    }

    @Test
    fun `saved id not in registry falls back to default theme`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        advanceUntilIdle()
        val provider = buildProvider(
            manifests = mapOf("studio" to studioJson),
            dispatcher = dispatcher,
            scope = this,
            savedId = "deleted-theme"
        )
        assertEquals("studio", provider.activeTheme.value.id)
    }

    @Test
    fun `setActiveTheme to already-active id still persists to prefs`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        advanceUntilIdle()
        val prefs = FakeThemePreferences()
        val registry = ThemeRegistryImpl(
            source = FakeThemeAssetSource(manifests = mapOf("studio" to studioJson)),
            ioDispatcher = dispatcher,
            scope = this
        )
        registry.refresh()
        val provider = ThemeProviderImpl(registry = registry, prefs = prefs)
        provider.setActiveTheme("studio")
        assertEquals("studio", prefs.lastId)
        assertEquals("studio", provider.activeTheme.value.id)
    }

    @Test
    fun `no isDefault theme and no saved id activates first available theme`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        advanceUntilIdle()
        val provider = buildProvider(
            manifests = mapOf("dojo" to dojoJson),
            dispatcher = dispatcher,
            scope = this,
            savedId = null
        )
        assertEquals("dojo", provider.activeTheme.value.id)
    }
}
