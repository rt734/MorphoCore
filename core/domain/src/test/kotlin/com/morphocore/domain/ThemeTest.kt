package com.morphocore.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeTest {

    private fun minimalTheme(id: String = "studio", isDefault: Boolean = false) = Theme(
        id = id,
        name = "Studio",
        description = "Clean studio",
        isDefault = isDefault,
        colors = MorphoColors(
            primary = 0xFF1565C0L,
            onPrimary = 0xFFFFFFFFL,
            secondary = 0xFF0097A7L,
            onSecondary = 0xFFFFFFFFL,
            background = 0xFFFAFAFAL,
            onBackground = 0xFF1A1A1AL,
            surface = 0xFFFFFFFFL,
            onSurface = 0xFF1A1A1AL,
            surfaceVariant = 0xFFF0F0F0L,
            onSurfaceVariant = 0xFF555555L,
            outline = 0xFFAAAAAAL,
            semantic = mapOf("discipline.karate" to 0xFFD32F2FL)
        ),
        typography = MorphoTypography(
            displayFontPath = null, bodyFontPath = null, labelFontPath = null,
            scale = mapOf("bodyLarge" to TextScaleEntry(16, 400, 24))
        ),
        shapes = MorphoShapes(smallDp = 4f, mediumDp = 12f, largeDp = 16f),
        motion = MorphoMotion(150, 300, 500, "cubicBezier(0.2, 0.0, 0.0, 1.0)"),
        scene = SceneConfig(
            skyboxPath = null,
            iblEnvironmentPath = "environments/studio_ibl.ktx2",
            iblIntensity = 1.0f,
            directLight = DirectLightConfig(0xFFFFFFFFL, 50000f, 0f, -45f),
            ambientIntensity = 0.5f,
            groundPlane = GroundPlaneConfig(false, 0xFFFFFFFFL, 0f),
            postProcessing = PostProcessingConfig(0f, 0f, "LINEAR")
        )
    )

    @Test
    fun `theme constructs with all required fields`() {
        val t = minimalTheme()
        assertEquals("studio", t.id)
        assertFalse(t.isDefault)
    }

    @Test
    fun `isDefault flag is preserved`() {
        assertTrue(minimalTheme(isDefault = true).isDefault)
    }

    @Test
    fun `semantic color map is accessible`() {
        val t = minimalTheme()
        assertEquals(0xFFD32F2FL, t.colors.semantic["discipline.karate"])
    }

    @Test
    fun `absent semantic key returns null`() {
        val t = minimalTheme()
        assertEquals(null, t.colors.semantic["discipline.nonexistent"])
    }
}
