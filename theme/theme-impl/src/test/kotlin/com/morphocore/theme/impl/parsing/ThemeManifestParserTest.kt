package com.morphocore.theme.impl.parsing

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ThemeManifestParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name"))
            .bufferedReader().readText()

    private val studioJson = fixture("studio-theme.json")
    private val dojoJson   = fixture("dojo-theme.json")
    private val neonJson   = fixture("neon-theme.json")

    @Test
    fun `parses studio fixture id and displayName`() {
        val result = parseTheme("test:studio", studioJson)
        assertIs<ThemeParseResult.Success>(result)
        assertEquals("studio", result.theme.id)
        assertEquals("Studio", result.theme.name)
    }

    @Test
    fun `studio isDefault is true`() {
        val result = parseTheme("test:studio", studioJson) as ThemeParseResult.Success
        assertTrue(result.theme.isDefault)
    }

    @Test
    fun `dojo isDefault is false`() {
        val result = parseTheme("test:dojo", dojoJson) as ThemeParseResult.Success
        assertTrue(!result.theme.isDefault)
    }

    @Test
    fun `parses primary color as ARGB Long`() {
        val result = parseTheme("test:studio", studioJson) as ThemeParseResult.Success
        // #1565C0 → 0xFF1565C0
        assertEquals(0xFF1565C0L, result.theme.colors.primary)
    }

    @Test
    fun `parses semantic color map`() {
        val result = parseTheme("test:studio", studioJson) as ThemeParseResult.Success
        assertNotNull(result.theme.colors.semantic["discipline.karate"])
        assertEquals(0xFFD32F2FL, result.theme.colors.semantic["discipline.karate"])
    }

    @Test
    fun `parses scene iblEnvironmentPath`() {
        val result = parseTheme("test:studio", studioJson) as ThemeParseResult.Success
        assertEquals("environments/studio_ibl.ktx2", result.theme.scene.iblEnvironmentPath)
    }

    @Test
    fun `parses directLight intensityLux`() {
        val result = parseTheme("test:dojo", dojoJson) as ThemeParseResult.Success
        assertEquals(80000.0f, result.theme.scene.directLight.intensityLux)
    }

    @Test
    fun `null skyboxPath is preserved as null`() {
        val result = parseTheme("test:studio", studioJson) as ThemeParseResult.Success
        assertEquals(null, result.theme.scene.skyboxPath)
    }

    @Test
    fun `non-null skyboxPath is parsed`() {
        val result = parseTheme("test:dojo", dojoJson) as ThemeParseResult.Success
        assertEquals("environments/dojo_skybox.ktx2", result.theme.scene.skyboxPath)
    }

    @Test
    fun `parses shapes corner radii`() {
        val result = parseTheme("test:studio", studioJson) as ThemeParseResult.Success
        assertEquals(4f, result.theme.shapes.smallDp)
        assertEquals(12f, result.theme.shapes.mediumDp)
        assertEquals(16f, result.theme.shapes.largeDp)
    }

    @Test
    fun `parses motion durations`() {
        val result = parseTheme("test:studio", studioJson) as ThemeParseResult.Success
        assertEquals(150, result.theme.motion.durationShortMs)
        assertEquals(300, result.theme.motion.durationMediumMs)
        assertEquals(500, result.theme.motion.durationLongMs)
    }

    @Test
    fun `neon postProcessing bloomIntensity parsed`() {
        val result = parseTheme("test:neon", neonJson) as ThemeParseResult.Success
        assertEquals(0.8f, result.theme.scene.postProcessing.bloomIntensity)
    }

    @Test
    fun `malformed JSON returns Failure not exception`() {
        val result = parseTheme("test:bad", "{ not json }")
        assertIs<ThemeParseResult.Failure>(result)
    }

    @Test
    fun `wrong schemaVersion returns Failure`() {
        val json = """{"schemaVersion":99,"id":"x","displayName":"X","colors":{"primary":"#000000","onPrimary":"#FFFFFF","background":"#FFFFFF","onBackground":"#000000"},"scene":{"iblEnvironmentPath":"x.ktx2","directLight":{"colorHex":"#FFFFFF","intensityLux":1000.0,"azimuthDegrees":0.0,"elevationDegrees":-45.0}}}"""
        val result = parseTheme("test:bad", json)
        assertIs<ThemeParseResult.Failure>(result)
    }

    @Test
    fun `failure carries source path`() {
        val result = parseTheme("source:themes/bad/theme.json", "bad") as ThemeParseResult.Failure
        assertEquals("source:themes/bad/theme.json", result.error.path)
    }

    @Test
    fun `hexToArgbLong converts six-char hex`() {
        assertEquals(0xFF1565C0L, hexToArgbLong("#1565C0"))
    }

    @Test
    fun `hexToArgbLong handles uppercase`() {
        assertEquals(0xFFFFFFFFL, hexToArgbLong("#FFFFFF"))
    }
}
