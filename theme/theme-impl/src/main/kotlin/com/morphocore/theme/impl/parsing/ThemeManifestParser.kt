package com.morphocore.theme.impl.parsing

import com.morphocore.domain.DirectLightConfig
import com.morphocore.domain.GroundPlaneConfig
import com.morphocore.domain.MorphoColors
import com.morphocore.domain.MorphoMotion
import com.morphocore.domain.MorphoShapes
import com.morphocore.domain.MorphoTypography
import com.morphocore.domain.PostProcessingConfig
import com.morphocore.domain.SceneConfig
import com.morphocore.domain.TextScaleEntry
import com.morphocore.domain.Theme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class ThemeDto(
    val schemaVersion: Int,
    val id: String,
    val displayName: String,
    val description: String = "",
    val isDefault: Boolean = false,
    val colors: ColorsDtoV2,
    val typography: TypographyDto = TypographyDto(),
    val shapes: ShapesDto = ShapesDto(),
    val motion: MotionDto = MotionDto(),
    val scene: SceneDto
)

@Serializable
internal data class ColorsDtoV2(
    val primary: String,
    val onPrimary: String,
    val secondary: String = "#888888",
    val onSecondary: String = "#FFFFFF",
    val background: String,
    val onBackground: String,
    val surface: String = "#FFFFFF",
    val onSurface: String = "#000000",
    val surfaceVariant: String = "#F0F0F0",
    val onSurfaceVariant: String = "#555555",
    val outline: String = "#AAAAAA",
    val semantic: Map<String, String> = emptyMap()
)

@Serializable
internal data class TypographyDto(
    val fontFamily: FontFamilyDto = FontFamilyDto(),
    val scale: Map<String, TextScaleDto> = emptyMap()
)

@Serializable
internal data class FontFamilyDto(
    val display: String? = null,
    val body: String? = null,
    val label: String? = null
)

@Serializable
internal data class TextScaleDto(val size: Int, val weight: Int, val lineHeight: Int)

@Serializable
internal data class ShapesDto(
    val small: Float = 4f,
    val medium: Float = 8f,
    val large: Float = 16f
)

@Serializable
internal data class MotionDto(
    val durationShort: Int = 150,
    val durationMedium: Int = 250,
    val durationLong: Int = 400,
    val easingStandard: String = "cubicBezier(0.2, 0.0, 0.0, 1.0)"
)

@Serializable
internal data class SceneDto(
    val skyboxPath: String? = null,
    val iblEnvironmentPath: String,
    val iblIntensity: Float = 1.0f,
    val directLight: DirectLightDto,
    val ambientIntensity: Float = 0.3f,
    val groundPlane: GroundPlaneDto = GroundPlaneDto(),
    val postProcessing: PostProcessingDto = PostProcessingDto()
)

@Serializable
internal data class DirectLightDto(
    val colorHex: String,
    val intensityLux: Float,
    val azimuthDegrees: Float,
    val elevationDegrees: Float
)

@Serializable
internal data class GroundPlaneDto(
    val enabled: Boolean = false,
    val colorHex: String = "#000000",
    val opacity: Float = 0f
)

@Serializable
internal data class PostProcessingDto(
    val bloomIntensity: Float = 0f,
    val vignetteIntensity: Float = 0f,
    val toneMapping: String = "ACES"
)

data class ThemeParseError(val path: String, val cause: Throwable)

sealed class ThemeParseResult {
    data class Success(val theme: Theme) : ThemeParseResult()
    data class Failure(val error: ThemeParseError) : ThemeParseResult()
}

private val jsonParser = Json { ignoreUnknownKeys = true }

internal fun parseTheme(path: String, jsonString: String): ThemeParseResult =
    try {
        val dto = jsonParser.decodeFromString<ThemeDto>(jsonString)
        if (dto.schemaVersion != 1) {
            return ThemeParseResult.Failure(
                ThemeParseError(
                    path,
                    IllegalArgumentException("Unsupported schemaVersion: ${dto.schemaVersion}")
                )
            )
        }
        ThemeParseResult.Success(dto.toTheme())
    } catch (e: Exception) {
        ThemeParseResult.Failure(ThemeParseError(path, e))
    }

internal fun hexToArgbLong(hex: String): Long {
    val cleaned = hex.trimStart('#')
    return when (cleaned.length) {
        6 -> 0xFF000000L or cleaned.toLong(16)
        8 -> cleaned.toLong(16)
        else -> throw IllegalArgumentException("Invalid hex color: '$hex'")
    }
}

private fun ThemeDto.toTheme() = Theme(
    id = id,
    name = displayName,
    description = description,
    isDefault = isDefault,
    colors = colors.toMorphoColors(),
    typography = typography.toMorphoTypography(),
    shapes = MorphoShapes(shapes.small, shapes.medium, shapes.large),
    motion = MorphoMotion(motion.durationShort, motion.durationMedium, motion.durationLong, motion.easingStandard),
    scene = scene.toSceneConfig()
)

private fun ColorsDtoV2.toMorphoColors() = MorphoColors(
    primary          = hexToArgbLong(primary),
    onPrimary        = hexToArgbLong(onPrimary),
    secondary        = hexToArgbLong(secondary),
    onSecondary      = hexToArgbLong(onSecondary),
    background       = hexToArgbLong(background),
    onBackground     = hexToArgbLong(onBackground),
    surface          = hexToArgbLong(surface),
    onSurface        = hexToArgbLong(onSurface),
    surfaceVariant   = hexToArgbLong(surfaceVariant),
    onSurfaceVariant = hexToArgbLong(onSurfaceVariant),
    outline          = hexToArgbLong(outline),
    semantic         = semantic.mapValues { (_, hex) -> hexToArgbLong(hex) }
)

private fun TypographyDto.toMorphoTypography() = MorphoTypography(
    displayFontPath = fontFamily.display,
    bodyFontPath    = fontFamily.body,
    labelFontPath   = fontFamily.label,
    scale           = scale.mapValues { (_, s) -> TextScaleEntry(s.size, s.weight, s.lineHeight) }
)

private fun SceneDto.toSceneConfig() = SceneConfig(
    skyboxPath           = skyboxPath,
    iblEnvironmentPath   = iblEnvironmentPath,
    iblIntensity         = iblIntensity,
    directLight          = DirectLightConfig(
        color            = hexToArgbLong(directLight.colorHex),
        intensityLux     = directLight.intensityLux,
        azimuthDegrees   = directLight.azimuthDegrees,
        elevationDegrees = directLight.elevationDegrees
    ),
    ambientIntensity = ambientIntensity,
    groundPlane      = GroundPlaneConfig(
        enabled = groundPlane.enabled,
        color   = hexToArgbLong(groundPlane.colorHex),
        opacity = groundPlane.opacity
    ),
    postProcessing = PostProcessingConfig(
        bloomIntensity    = postProcessing.bloomIntensity,
        vignetteIntensity = postProcessing.vignetteIntensity,
        toneMapping       = postProcessing.toneMapping
    )
)
