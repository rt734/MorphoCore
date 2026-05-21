package com.morphocore.theme.impl.parsing

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Reads the live theme asset files from app/src/main/assets/themes/ and asserts
 * structural invariants. Gradle sets the JVM test working directory to the module
 * root (theme/theme-impl/), so ../../app/src/main/assets/themes resolves correctly.
 */
class ThemeManifestIntegrityTest {

    private val themesRoot = File("../../app/src/main/assets/themes")

    private fun readTheme(themeId: String): ThemeParseResult {
        val file = themesRoot.resolve("$themeId/theme.json")
        assumeTrue(file.exists(), "Skipping: theme not found at ${file.absolutePath}")
        return parseTheme("themes/$themeId/theme.json", file.readText())
    }

    @ParameterizedTest
    @ValueSource(strings = ["dojo", "iron", "studio", "neon"])
    fun `theme parses without error`(themeId: String) {
        val result = readTheme(themeId)
        assertIs<ThemeParseResult.Success>(result, "Parse failed for $themeId")
    }

    @ParameterizedTest
    @ValueSource(strings = ["dojo", "iron", "studio", "neon"])
    fun `theme id matches directory name`(themeId: String) {
        val result = readTheme(themeId) as ThemeParseResult.Success
        assertEquals(themeId, result.theme.id, "$themeId theme.id mismatch")
    }

    @ParameterizedTest
    @ValueSource(strings = ["dojo", "iron", "studio", "neon"])
    fun `theme has non-blank name and description`(themeId: String) {
        val result = readTheme(themeId) as ThemeParseResult.Success
        assertTrue(result.theme.name.isNotBlank(), "$themeId: name is blank")
        assertTrue(result.theme.description.isNotBlank(), "$themeId: description is blank")
    }

    @ParameterizedTest
    @ValueSource(strings = ["dojo", "iron", "studio", "neon"])
    fun `all color values are non-zero`(themeId: String) {
        val result = readTheme(themeId) as ThemeParseResult.Success
        val colors = result.theme.colors
        val fields = listOf(
            colors.primary, colors.onPrimary, colors.secondary, colors.onSecondary,
            colors.background, colors.onBackground, colors.surface, colors.onSurface,
            colors.surfaceVariant, colors.onSurfaceVariant, colors.outline
        )
        fields.forEach { value ->
            assertTrue(value != 0L, "$themeId has a zero color value")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["dojo", "iron", "studio", "neon"])
    fun `scene config has positive ibl intensity and ambient intensity`(themeId: String) {
        val result = readTheme(themeId) as ThemeParseResult.Success
        val scene = result.theme.scene
        assertTrue(scene.iblIntensity > 0f, "$themeId: iblIntensity must be positive")
        assertTrue(scene.ambientIntensity > 0f, "$themeId: ambientIntensity must be positive")
    }

    @ParameterizedTest
    @ValueSource(strings = ["dojo", "iron", "studio", "neon"])
    fun `motion durations are positive and in ascending order`(themeId: String) {
        val result = readTheme(themeId) as ThemeParseResult.Success
        val motion = result.theme.motion
        assertTrue(motion.durationShortMs > 0, "$themeId: durationShortMs must be positive")
        assertTrue(motion.durationMediumMs >= motion.durationShortMs,
            "$themeId: medium duration must be >= short")
        assertTrue(motion.durationLongMs >= motion.durationMediumMs,
            "$themeId: long duration must be >= medium")
    }

    @ParameterizedTest
    @ValueSource(strings = ["dojo", "iron", "studio", "neon"])
    fun `exactly one theme is marked as default`(themeId: String) {
        // Each individual theme is tested; the "exactly one" invariant is checked
        // across all four by asserting dojo (the design-time default) is the only one.
        val result = readTheme(themeId) as ThemeParseResult.Success
        val isDefault = result.theme.isDefault
        val shouldBeDefault = themeId == "dojo"
        assertEquals(shouldBeDefault, isDefault,
            "$themeId isDefault=$isDefault but shouldBeDefault=$shouldBeDefault")
    }
}
