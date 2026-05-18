# Sprint 2 — Rendering Integration + Theme System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement SceneView rendering abstraction, FakeSceneViewport test double, theme manifest parsing pipeline, and MorphoTheme Compose wrapper.

**Architecture:** Theme domain types are expanded to carry the full manifest schema; ThemeManifestParser converts JSON → Theme; MorphoTheme converts Theme → MaterialTheme + CompositionLocal tokens. SceneViewportImpl wraps SceneView via an AndroidView Composable; FakeSceneViewport is a pure-Kotlin call-recorder for ViewModel tests. No Java/Gradle execution available — all steps produce code only; test runs require Android Studio JDK.

**Tech Stack:** Kotlin 2.0.21 · SceneView 2.2.1 · Compose BOM 2024.09.03 · Material3 · kotlinx.serialization · kotlinx.coroutines-test · JUnit 5

---

## File map

```
# Build config
gradle/libs.versions.toml                                              MODIFY
build-logic/src/main/kotlin/morphocore.compose.library.gradle.kts     CREATE

# Domain expansion
core/domain/src/main/kotlin/com/morphocore/domain/Theme.kt             REWRITE
core/domain/src/main/kotlin/com/morphocore/domain/ColorTokens.kt       DELETE
core/domain/src/main/kotlin/com/morphocore/domain/MorphoColors.kt      CREATE
core/domain/src/main/kotlin/com/morphocore/domain/MorphoTypography.kt  CREATE
core/domain/src/main/kotlin/com/morphocore/domain/MorphoShapes.kt      CREATE
core/domain/src/main/kotlin/com/morphocore/domain/MorphoMotion.kt      CREATE
core/domain/src/main/kotlin/com/morphocore/domain/SceneConfig.kt       CREATE
core/domain/src/test/kotlin/com/morphocore/domain/ThemeTest.kt         CREATE

# Schemas + fixtures
schemas/theme-manifest.schema.json                                     REWRITE
schemas/fixtures/dojo-theme.json                                       REWRITE
schemas/fixtures/iron-theme.json                                       REWRITE
schemas/fixtures/studio-theme.json                                     CREATE
schemas/fixtures/neon-theme.json                                       CREATE
theme/theme-impl/src/test/resources/fixtures/dojo-theme.json          CREATE
theme/theme-impl/src/test/resources/fixtures/iron-theme.json          CREATE
theme/theme-impl/src/test/resources/fixtures/studio-theme.json        CREATE
theme/theme-impl/src/test/resources/fixtures/neon-theme.json          CREATE

# theme-api additions
theme/theme-api/src/main/kotlin/com/morphocore/theme/api/ThemeAssetSource.kt  CREATE

# theme-impl
theme/theme-impl/build.gradle.kts                                      MODIFY
theme/theme-impl/src/main/AndroidManifest.xml                          keep (unchanged)
theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/parsing/ThemeManifestParser.kt  CREATE
theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/registry/ThemeRegistryImpl.kt   CREATE
theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/registry/BundledThemeAssetSource.kt  CREATE
theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/provider/ThemePreferences.kt    CREATE
theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/provider/ThemeProviderImpl.kt   CREATE
theme/theme-impl/src/test/kotlin/com/morphocore/theme/impl/FakeThemeAssetSource.kt         CREATE
theme/theme-impl/src/test/kotlin/com/morphocore/theme/impl/parsing/ThemeManifestParserTest.kt  CREATE
theme/theme-impl/src/test/kotlin/com/morphocore/theme/impl/registry/ThemeRegistryImplTest.kt   CREATE
theme/theme-impl/src/test/kotlin/com/morphocore/theme/impl/provider/ThemeProviderImplTest.kt   CREATE

# core/design-system
core/design-system/build.gradle.kts                                    MODIFY
core/design-system/src/main/AndroidManifest.xml                        keep (unchanged)
core/design-system/src/main/kotlin/com/morphocore/designsystem/ResolvedMorphoTokens.kt  CREATE
core/design-system/src/main/kotlin/com/morphocore/designsystem/LocalMorphoTheme.kt      CREATE
core/design-system/src/main/kotlin/com/morphocore/designsystem/MorphoTheme.kt           CREATE
core/design-system/src/main/kotlin/com/morphocore/designsystem/ColorExtensions.kt       CREATE
core/design-system/src/main/kotlin/com/morphocore/designsystem/TypographyExtensions.kt  CREATE
core/design-system/src/main/kotlin/com/morphocore/designsystem/ShapeExtensions.kt       CREATE

# rendering-api addition
rendering/rendering-api/src/main/kotlin/com/morphocore/rendering/api/LoadedModelFactory.kt  CREATE

# rendering-testing (converted to pure-Kotlin)
rendering/rendering-testing/build.gradle.kts                           REWRITE
rendering/rendering-testing/src/main/AndroidManifest.xml               DELETE
rendering/rendering-testing/src/main/kotlin/com/morphocore/rendering/testing/ViewportCall.kt       CREATE
rendering/rendering-testing/src/main/kotlin/com/morphocore/rendering/testing/FakeSceneViewport.kt  CREATE
rendering/rendering-testing/src/test/kotlin/com/morphocore/rendering/testing/FakeSceneViewportTest.kt  CREATE

# rendering-scene-view
rendering/rendering-scene-view/build.gradle.kts                        MODIFY
rendering/rendering-scene-view/src/main/kotlin/com/morphocore/rendering/sceneview/SceneViewportImpl.kt      CREATE
rendering/rendering-scene-view/src/main/kotlin/com/morphocore/rendering/sceneview/SceneViewportSurface.kt   CREATE

# app
app/build.gradle.kts                                                   MODIFY
app/src/main/AndroidManifest.xml                                       MODIFY
app/src/main/kotlin/com/morphocore/app/SmokeTestActivity.kt           CREATE
```

---

## Task 1: Build config — libs.versions.toml + morphocore.compose.library

**Files:**
- Modify: `gradle/libs.versions.toml`
- Create: `build-logic/src/main/kotlin/morphocore.compose.library.gradle.kts`

- [ ] **Step 1: Add new entries to `gradle/libs.versions.toml`**

Open `gradle/libs.versions.toml` and add the following entries (insert under existing sections):

In `[versions]`:
```toml
sceneview      = "2.2.1"
compose-bom    = "2024.09.03"
datastore      = "1.1.1"
```

In `[libraries]` (after existing entries):
```toml
compose-bom              = { module = "androidx.compose:compose-bom",                 version.ref = "compose-bom" }
compose-ui               = { module = "androidx.compose.ui:ui" }
compose-material3        = { module = "androidx.compose.material3:material3" }
compose-activity         = { module = "androidx.activity:activity-compose",            version = "1.9.2" }
sceneview                = { module = "io.github.sceneview:sceneview",                 version.ref = "sceneview" }
datastore-preferences    = { module = "androidx.datastore:datastore-preferences",      version.ref = "datastore" }
```

Note: `compose-ui` and `compose-material3` have no `version.ref` — they are version-managed by the Compose BOM at runtime.

In `[plugins]` (after existing entries):
```toml
kotlin-compose-compiler  = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

- [ ] **Step 2: Add the Compose compiler plugin dependency to `build-logic/build.gradle.kts`**

The current file is:
```kotlin
plugins {
    `kotlin-dsl`
}
dependencies {
    implementation(libs.gradlePlugin.android)
    implementation(libs.gradlePlugin.kotlin)
}
```

Replace with:
```kotlin
plugins {
    `kotlin-dsl`
}
dependencies {
    implementation(libs.gradlePlugin.android)
    implementation(libs.gradlePlugin.kotlin)
}
```

No change needed here — the `kotlin-compose-compiler` plugin is applied in the convention plugin itself using `alias(libs.plugins.kotlin.compose.compiler)`. The convention plugin file handles it.

- [ ] **Step 3: Create `build-logic/src/main/kotlin/morphocore.compose.library.gradle.kts`**

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation(kotlin("test"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

- [ ] **Step 4: Commit**

```powershell
cd "C:\Users\HP\Documents\Claude\Projects\MorphoCore"
git add gradle/libs.versions.toml build-logic/
git commit -m "chore: add Compose + SceneView deps and morphocore.compose.library convention plugin"
```

---

## Task 2: Domain expansion — replace Theme type tree

**Files:**
- Rewrite: `core/domain/src/main/kotlin/com/morphocore/domain/Theme.kt`
- Delete: `core/domain/src/main/kotlin/com/morphocore/domain/ColorTokens.kt`
- Create: `core/domain/src/main/kotlin/com/morphocore/domain/MorphoColors.kt`
- Create: `core/domain/src/main/kotlin/com/morphocore/domain/MorphoTypography.kt`
- Create: `core/domain/src/main/kotlin/com/morphocore/domain/MorphoShapes.kt`
- Create: `core/domain/src/main/kotlin/com/morphocore/domain/MorphoMotion.kt`
- Create: `core/domain/src/main/kotlin/com/morphocore/domain/SceneConfig.kt`
- Create: `core/domain/src/test/kotlin/com/morphocore/domain/ThemeTest.kt`

- [ ] **Step 1: Delete `ColorTokens.kt`**

```powershell
Remove-Item "C:\Users\HP\Documents\Claude\Projects\MorphoCore\core\domain\src\main\kotlin\com\morphocore\domain\ColorTokens.kt"
```

- [ ] **Step 2: Rewrite `Theme.kt`**

```kotlin
package com.morphocore.domain

data class Theme(
    val id: String,
    val name: String,
    val description: String,
    val isDefault: Boolean,
    val colors: MorphoColors,
    val typography: MorphoTypography,
    val shapes: MorphoShapes,
    val motion: MorphoMotion,
    val scene: SceneConfig
)
```

- [ ] **Step 3: Create `MorphoColors.kt`**

```kotlin
package com.morphocore.domain

// All color values are ARGB Long (0xFFRRGGBB). Parsed from #RRGGBB hex strings.
data class MorphoColors(
    val primary: Long,
    val onPrimary: Long,
    val secondary: Long,
    val onSecondary: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val outline: Long,
    // Keys: "discipline.<id>" or "difficulty.<level>". Absent key = fall back to primary.
    val semantic: Map<String, Long>
)
```

- [ ] **Step 4: Create `MorphoTypography.kt`**

```kotlin
package com.morphocore.domain

data class MorphoTypography(
    // Relative paths within the theme folder, e.g. "fonts/NotoSerifJP-Bold.ttf". Null = system font.
    val displayFontPath: String?,
    val bodyFontPath: String?,
    val labelFontPath: String?,
    // Keys: "displayLarge", "headlineLarge", "titleLarge", "bodyLarge", "labelLarge"
    val scale: Map<String, TextScaleEntry>
)

data class TextScaleEntry(val sizeSp: Int, val weight: Int, val lineHeightSp: Int)
```

- [ ] **Step 5: Create `MorphoShapes.kt`**

```kotlin
package com.morphocore.domain

data class MorphoShapes(
    val smallDp: Float,
    val mediumDp: Float,
    val largeDp: Float
)
```

- [ ] **Step 6: Create `MorphoMotion.kt`**

```kotlin
package com.morphocore.domain

data class MorphoMotion(
    val durationShortMs: Int,
    val durationMediumMs: Int,
    val durationLongMs: Int,
    val easingStandard: String
)
```

- [ ] **Step 7: Create `SceneConfig.kt`**

```kotlin
package com.morphocore.domain

data class SceneConfig(
    val skyboxPath: String?,
    val iblEnvironmentPath: String,
    val iblIntensity: Float,
    val directLight: DirectLightConfig,
    val ambientIntensity: Float,
    val groundPlane: GroundPlaneConfig,
    val postProcessing: PostProcessingConfig
)

data class DirectLightConfig(
    val color: Long,          // ARGB
    val intensityLux: Float,
    val azimuthDegrees: Float,
    val elevationDegrees: Float
)

data class GroundPlaneConfig(
    val enabled: Boolean,
    val color: Long,
    val opacity: Float
)

data class PostProcessingConfig(
    val bloomIntensity: Float,
    val vignetteIntensity: Float,
    val toneMapping: String   // "ACES", "LINEAR", "FILMIC"
)
```

- [ ] **Step 8: Create `ThemeTest.kt`**

```kotlin
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
```

- [ ] **Step 9: Commit**

```powershell
git add core/domain/
git commit -m "feat(domain): expand Theme type tree — replace ColorTokens with MorphoColors + full token types

Adds MorphoColors, MorphoTypography, MorphoShapes, MorphoMotion, SceneConfig
and supporting value types. ColorTokens removed — superseded by MorphoColors."
```

---

## Task 3: Theme manifest schema + all four fixtures

**Files:**
- Rewrite: `schemas/theme-manifest.schema.json`
- Rewrite: `schemas/fixtures/dojo-theme.json`
- Rewrite: `schemas/fixtures/iron-theme.json`
- Create: `schemas/fixtures/studio-theme.json`
- Create: `schemas/fixtures/neon-theme.json`
- Create: `theme/theme-impl/src/test/resources/fixtures/` (4 files, copies of above)

- [ ] **Step 1: Rewrite `schemas/theme-manifest.schema.json`**

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "morphocore:theme-manifest:2",
  "title": "Theme Manifest v2",
  "type": "object",
  "required": ["schemaVersion", "id", "displayName", "colors", "scene"],
  "properties": {
    "schemaVersion": { "type": "integer", "const": 1 },
    "id":            { "type": "string", "pattern": "^[a-z][a-z0-9_]*$" },
    "displayName":   { "type": "string", "minLength": 1 },
    "description":   { "type": "string" },
    "isDefault":     { "type": "boolean" },
    "colors": {
      "type": "object",
      "required": ["primary", "onPrimary", "background", "onBackground"],
      "properties": {
        "primary":          { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" },
        "onPrimary":        { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" },
        "secondary":        { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" },
        "onSecondary":      { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" },
        "background":       { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" },
        "onBackground":     { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" },
        "surface":          { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" },
        "onSurface":        { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" },
        "surfaceVariant":   { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" },
        "onSurfaceVariant": { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" },
        "outline":          { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" },
        "semantic":         { "type": "object", "additionalProperties": { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" } }
      }
    },
    "typography": {
      "type": "object",
      "properties": {
        "fontFamily": {
          "type": "object",
          "properties": {
            "display": { "type": ["string", "null"] },
            "body":    { "type": ["string", "null"] },
            "label":   { "type": ["string", "null"] }
          }
        },
        "scale": {
          "type": "object",
          "additionalProperties": {
            "type": "object",
            "required": ["size", "weight", "lineHeight"],
            "properties": {
              "size":       { "type": "integer" },
              "weight":     { "type": "integer" },
              "lineHeight": { "type": "integer" }
            }
          }
        }
      }
    },
    "shapes": {
      "type": "object",
      "properties": {
        "small":  { "type": "number" },
        "medium": { "type": "number" },
        "large":  { "type": "number" }
      }
    },
    "motion": {
      "type": "object",
      "properties": {
        "durationShort":   { "type": "integer" },
        "durationMedium":  { "type": "integer" },
        "durationLong":    { "type": "integer" },
        "easingStandard":  { "type": "string" }
      }
    },
    "scene": {
      "type": "object",
      "required": ["iblEnvironmentPath", "directLight"],
      "properties": {
        "skyboxPath":         { "type": ["string", "null"] },
        "iblEnvironmentPath": { "type": "string" },
        "iblIntensity":       { "type": "number" },
        "directLight": {
          "type": "object",
          "required": ["colorHex", "intensityLux", "azimuthDegrees", "elevationDegrees"],
          "properties": {
            "colorHex":         { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" },
            "intensityLux":     { "type": "number" },
            "azimuthDegrees":   { "type": "number" },
            "elevationDegrees": { "type": "number" }
          }
        },
        "ambientIntensity": { "type": "number" },
        "groundPlane": {
          "type": "object",
          "properties": {
            "enabled":  { "type": "boolean" },
            "colorHex": { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" },
            "opacity":  { "type": "number" }
          }
        },
        "postProcessing": {
          "type": "object",
          "properties": {
            "bloomIntensity":     { "type": "number" },
            "vignetteIntensity":  { "type": "number" },
            "toneMapping":        { "type": "string", "enum": ["ACES", "LINEAR", "FILMIC"] }
          }
        }
      }
    }
  }
}
```

- [ ] **Step 2: Rewrite `schemas/fixtures/dojo-theme.json`**

```json
{
  "schemaVersion": 1,
  "id": "dojo",
  "displayName": "Dojo",
  "description": "Warm woods, paper textures, soft warm lighting.",
  "isDefault": false,
  "colors": {
    "primary": "#8B5A2B",
    "onPrimary": "#FFFFFF",
    "secondary": "#C9A36A",
    "onSecondary": "#1A0F00",
    "background": "#F5EFE3",
    "onBackground": "#1A0F00",
    "surface": "#FAF6EC",
    "onSurface": "#1A0F00",
    "surfaceVariant": "#E8DFCC",
    "onSurfaceVariant": "#3D2E1A",
    "outline": "#8C7B5A",
    "semantic": {
      "discipline.karate": "#A8412A",
      "discipline.kungfu": "#B8862A",
      "discipline.yoga": "#7A8C5A",
      "discipline.gym": "#5A6B7A",
      "discipline.calisthenics": "#8A6A4A",
      "difficulty.easy": "#7A8C5A",
      "difficulty.medium": "#C9A36A",
      "difficulty.hard": "#A8412A"
    }
  },
  "typography": {
    "fontFamily": { "display": "fonts/NotoSerifJP-Bold.ttf", "body": "fonts/NotoSerifJP-Regular.ttf", "label": "fonts/NotoSansJP-Medium.ttf" },
    "scale": {
      "displayLarge":  { "size": 57, "weight": 700, "lineHeight": 64 },
      "headlineLarge": { "size": 32, "weight": 700, "lineHeight": 40 },
      "titleLarge":    { "size": 22, "weight": 600, "lineHeight": 28 },
      "bodyLarge":     { "size": 16, "weight": 400, "lineHeight": 24 },
      "labelLarge":    { "size": 14, "weight": 500, "lineHeight": 20 }
    }
  },
  "shapes": { "small": 4, "medium": 8, "large": 16 },
  "motion": { "durationShort": 150, "durationMedium": 250, "durationLong": 400, "easingStandard": "cubicBezier(0.2, 0.0, 0.0, 1.0)" },
  "scene": {
    "skyboxPath": "environments/dojo_skybox.ktx2",
    "iblEnvironmentPath": "environments/dojo_ibl.ktx2",
    "iblIntensity": 1.0,
    "directLight": { "colorHex": "#FFE8C2", "intensityLux": 80000.0, "azimuthDegrees": 45.0, "elevationDegrees": -35.0 },
    "ambientIntensity": 0.3,
    "groundPlane": { "enabled": true, "colorHex": "#3D2E1A", "opacity": 0.4 },
    "postProcessing": { "bloomIntensity": 0.1, "vignetteIntensity": 0.15, "toneMapping": "ACES" }
  }
}
```

- [ ] **Step 3: Rewrite `schemas/fixtures/iron-theme.json`**

```json
{
  "schemaVersion": 1,
  "id": "iron",
  "displayName": "Iron",
  "description": "Dark charcoal surfaces, industrial red accents, hard rim lighting.",
  "isDefault": false,
  "colors": {
    "primary": "#CC2200", "onPrimary": "#FFFFFF",
    "secondary": "#8A9BAA", "onSecondary": "#0A0F14",
    "background": "#0D0F11", "onBackground": "#D8DEE4",
    "surface": "#1A1D20", "onSurface": "#D8DEE4",
    "surfaceVariant": "#2A2E33", "onSurfaceVariant": "#9BAAB8",
    "outline": "#4A5560",
    "semantic": {
      "discipline.karate": "#CC2200", "discipline.kungfu": "#CC6600",
      "discipline.yoga": "#66AA88", "discipline.gym": "#CC2200",
      "discipline.calisthenics": "#8888CC",
      "difficulty.easy": "#66AA88", "difficulty.medium": "#CC8800", "difficulty.hard": "#CC2200"
    }
  },
  "typography": {
    "fontFamily": { "display": null, "body": null, "label": null },
    "scale": {
      "displayLarge":  { "size": 57, "weight": 800, "lineHeight": 64 },
      "headlineLarge": { "size": 32, "weight": 800, "lineHeight": 40 },
      "titleLarge":    { "size": 22, "weight": 700, "lineHeight": 28 },
      "bodyLarge":     { "size": 16, "weight": 400, "lineHeight": 24 },
      "labelLarge":    { "size": 14, "weight": 600, "lineHeight": 20 }
    }
  },
  "shapes": { "small": 2, "medium": 4, "large": 8 },
  "motion": { "durationShort": 100, "durationMedium": 200, "durationLong": 300, "easingStandard": "cubicBezier(0.2, 0.0, 0.0, 1.0)" },
  "scene": {
    "skyboxPath": null,
    "iblEnvironmentPath": "environments/iron_ibl.ktx2",
    "iblIntensity": 1.2,
    "directLight": { "colorHex": "#D0E8FF", "intensityLux": 120000.0, "azimuthDegrees": -30.0, "elevationDegrees": -20.0 },
    "ambientIntensity": 0.1,
    "groundPlane": { "enabled": true, "colorHex": "#0A0C0E", "opacity": 0.6 },
    "postProcessing": { "bloomIntensity": 0.05, "vignetteIntensity": 0.3, "toneMapping": "ACES" }
  }
}
```

- [ ] **Step 4: Create `schemas/fixtures/studio-theme.json`**

```json
{
  "schemaVersion": 1,
  "id": "studio",
  "displayName": "Studio",
  "description": "Clean photography studio — neutral default for all disciplines.",
  "isDefault": true,
  "colors": {
    "primary": "#1565C0", "onPrimary": "#FFFFFF",
    "secondary": "#0097A7", "onSecondary": "#FFFFFF",
    "background": "#FAFAFA", "onBackground": "#1A1A1A",
    "surface": "#FFFFFF", "onSurface": "#1A1A1A",
    "surfaceVariant": "#F0F0F0", "onSurfaceVariant": "#555555",
    "outline": "#AAAAAA",
    "semantic": {
      "discipline.karate": "#D32F2F", "discipline.kungfu": "#F57C00",
      "discipline.yoga": "#388E3C", "discipline.gym": "#1565C0",
      "discipline.calisthenics": "#7B1FA2",
      "difficulty.easy": "#388E3C", "difficulty.medium": "#F57C00", "difficulty.hard": "#D32F2F"
    }
  },
  "typography": {
    "fontFamily": { "display": null, "body": null, "label": null },
    "scale": {
      "displayLarge":  { "size": 57, "weight": 400, "lineHeight": 64 },
      "headlineLarge": { "size": 32, "weight": 400, "lineHeight": 40 },
      "titleLarge":    { "size": 22, "weight": 500, "lineHeight": 28 },
      "bodyLarge":     { "size": 16, "weight": 400, "lineHeight": 24 },
      "labelLarge":    { "size": 14, "weight": 500, "lineHeight": 20 }
    }
  },
  "shapes": { "small": 4, "medium": 12, "large": 16 },
  "motion": { "durationShort": 150, "durationMedium": 300, "durationLong": 500, "easingStandard": "cubicBezier(0.2, 0.0, 0.0, 1.0)" },
  "scene": {
    "skyboxPath": null,
    "iblEnvironmentPath": "environments/studio_ibl.ktx2",
    "iblIntensity": 1.0,
    "directLight": { "colorHex": "#FFFFFF", "intensityLux": 50000.0, "azimuthDegrees": 0.0, "elevationDegrees": -45.0 },
    "ambientIntensity": 0.5,
    "groundPlane": { "enabled": false, "colorHex": "#FFFFFF", "opacity": 0.0 },
    "postProcessing": { "bloomIntensity": 0.0, "vignetteIntensity": 0.0, "toneMapping": "LINEAR" }
  }
}
```

- [ ] **Step 5: Create `schemas/fixtures/neon-theme.json`**

```json
{
  "schemaVersion": 1,
  "id": "neon",
  "displayName": "Neon",
  "description": "Futuristic training space with bloom-heavy post-processing.",
  "isDefault": false,
  "colors": {
    "primary": "#00E5FF", "onPrimary": "#000000",
    "secondary": "#FF00FF", "onSecondary": "#000000",
    "background": "#050510", "onBackground": "#E0E0FF",
    "surface": "#0A0A20", "onSurface": "#E0E0FF",
    "surfaceVariant": "#151530", "onSurfaceVariant": "#8888CC",
    "outline": "#3333AA",
    "semantic": {
      "discipline.karate": "#FF4488", "discipline.kungfu": "#FF8800",
      "discipline.yoga": "#00FF88", "discipline.gym": "#00E5FF",
      "discipline.calisthenics": "#AA00FF",
      "difficulty.easy": "#00FF88", "difficulty.medium": "#FFAA00", "difficulty.hard": "#FF0044"
    }
  },
  "typography": {
    "fontFamily": { "display": null, "body": null, "label": null },
    "scale": {
      "displayLarge":  { "size": 57, "weight": 700, "lineHeight": 64 },
      "headlineLarge": { "size": 32, "weight": 700, "lineHeight": 40 },
      "titleLarge":    { "size": 22, "weight": 600, "lineHeight": 28 },
      "bodyLarge":     { "size": 16, "weight": 300, "lineHeight": 24 },
      "labelLarge":    { "size": 14, "weight": 400, "lineHeight": 20 }
    }
  },
  "shapes": { "small": 0, "medium": 2, "large": 4 },
  "motion": { "durationShort": 80, "durationMedium": 180, "durationLong": 350, "easingStandard": "cubicBezier(0.0, 0.0, 0.2, 1.0)" },
  "scene": {
    "skyboxPath": "environments/neon_skybox.ktx2",
    "iblEnvironmentPath": "environments/neon_ibl.ktx2",
    "iblIntensity": 0.5,
    "directLight": { "colorHex": "#00FFFF", "intensityLux": 150000.0, "azimuthDegrees": 90.0, "elevationDegrees": -10.0 },
    "ambientIntensity": 0.05,
    "groundPlane": { "enabled": true, "colorHex": "#000033", "opacity": 0.8 },
    "postProcessing": { "bloomIntensity": 0.8, "vignetteIntensity": 0.5, "toneMapping": "ACES" }
  }
}
```

- [ ] **Step 6: Copy all four fixtures to theme-impl test resources**

```powershell
New-Item -ItemType Directory -Force "C:\Users\HP\Documents\Claude\Projects\MorphoCore\theme\theme-impl\src\test\resources\fixtures"
$src = "C:\Users\HP\Documents\Claude\Projects\MorphoCore\schemas\fixtures"
$dst = "C:\Users\HP\Documents\Claude\Projects\MorphoCore\theme\theme-impl\src\test\resources\fixtures"
Copy-Item "$src\dojo-theme.json"   $dst
Copy-Item "$src\iron-theme.json"   $dst
Copy-Item "$src\studio-theme.json" $dst
Copy-Item "$src\neon-theme.json"   $dst
```

- [ ] **Step 7: Commit**

```powershell
git add schemas/ theme/theme-impl/src/test/resources/
git commit -m "feat: rewrite theme manifest schema v2 and add all four theme fixtures

Dojo, Iron, Studio (default), Neon. Full colors/typography/shapes/motion/scene.
Copies placed in theme-impl test resources for JVM classpath access."
```

---

## Task 4: ThemeAssetSource in theme-api + build.gradle.kts for theme-impl

**Files:**
- Create: `theme/theme-api/src/main/kotlin/com/morphocore/theme/api/ThemeAssetSource.kt`
- Modify: `theme/theme-impl/build.gradle.kts`

- [ ] **Step 1: Create `ThemeAssetSource.kt`**

```kotlin
package com.morphocore.theme.api

interface ThemeAssetSource {
    val id: String
    suspend fun listThemeIds(): List<String>
    // Returns raw JSON string for the theme manifest, or null if unavailable.
    suspend fun readThemeManifest(themeId: String): String?
}
```

- [ ] **Step 2: Update `theme/theme-impl/build.gradle.kts`**

```kotlin
plugins { id("morphocore.android.library") }

android {
    namespace = "com.morphocore.theme.impl"
}

dependencies {
    implementation(project(":theme:theme-api"))
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
```

Add `alias(libs.plugins.kotlin.serialization)` plugin too:

```kotlin
plugins {
    id("morphocore.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.morphocore.theme.impl"
}

dependencies {
    implementation(project(":theme:theme-api"))
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
```

- [ ] **Step 3: Commit**

```powershell
git add theme/theme-api/src/ theme/theme-impl/build.gradle.kts
git commit -m "feat(theme-api): add ThemeAssetSource interface; configure theme-impl build"
```

---

## Task 5: ThemeManifestParser — TDD

**Files:**
- Create: `theme/theme-impl/src/test/kotlin/com/morphocore/theme/impl/FakeThemeAssetSource.kt`
- Create: `theme/theme-impl/src/test/kotlin/com/morphocore/theme/impl/parsing/ThemeManifestParserTest.kt`
- Create: `theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/parsing/ThemeManifestParser.kt`

- [ ] **Step 1: Create `FakeThemeAssetSource.kt`**

```kotlin
package com.morphocore.theme.impl

import com.morphocore.theme.api.ThemeAssetSource

class FakeThemeAssetSource(
    override val id: String = "fake",
    private val manifests: Map<String, String> = emptyMap()
) : ThemeAssetSource {
    override suspend fun listThemeIds(): List<String> = manifests.keys.toList()
    override suspend fun readThemeManifest(themeId: String): String? = manifests[themeId]
}
```

- [ ] **Step 2: Create `ThemeManifestParserTest.kt`**

```kotlin
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
```

- [ ] **Step 3: Run to confirm compilation failure** *(requires Android Studio JDK)*

```powershell
.\gradlew :theme:theme-impl:test --tests "*.ThemeManifestParserTest"
```
Expected: BUILD FAILED — `parseTheme`, `ThemeParseResult`, `hexToArgbLong` not found.

- [ ] **Step 4: Create `ThemeManifestParser.kt`**

```kotlin
package com.morphocore.theme.impl.parsing

import com.morphocore.domain.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ── DTOs ─────────────────────────────────────────────────────────────────────

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

// ── Parse result ──────────────────────────────────────────────────────────────

data class ThemeParseError(val path: String, val cause: Throwable)

sealed class ThemeParseResult {
    data class Success(val theme: Theme) : ThemeParseResult()
    data class Failure(val error: ThemeParseError) : ThemeParseResult()
}

// ── Public API ────────────────────────────────────────────────────────────────

private val jsonParser = Json { ignoreUnknownKeys = true }

internal fun parseTheme(path: String, jsonString: String): ThemeParseResult =
    try {
        val dto = jsonParser.decodeFromString<ThemeDto>(jsonString)
        if (dto.schemaVersion != 1) {
            return ThemeParseResult.Failure(
                ThemeParseError(path, IllegalArgumentException("Unsupported schemaVersion: ${dto.schemaVersion}"))
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

// ── DTO → Domain conversion ───────────────────────────────────────────────────

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
```

- [ ] **Step 5: Run tests** *(requires JDK)*

```powershell
.\gradlew :theme:theme-impl:test --tests "*.ThemeManifestParserTest"
```
Expected: BUILD SUCCESSFUL, 17 tests PASSED.

- [ ] **Step 6: Commit**

```powershell
git add theme/theme-impl/src/
git commit -m "feat(theme-impl): add ThemeManifestParser with full DTO tree and 17 unit tests

Converts JSON theme manifests to Theme domain objects. hexToArgbLong converts
#RRGGBB strings to ARGB Long. Partial-field defaults applied via DTO defaults."
```

---

## Task 6: ThemeRegistryImpl — TDD

**Files:**
- Create: `theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/registry/BundledThemeAssetSource.kt`
- Create: `theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/registry/ThemeRegistryImpl.kt`
- Create: `theme/theme-impl/src/test/kotlin/com/morphocore/theme/impl/registry/ThemeRegistryImplTest.kt`

- [ ] **Step 1: Create `BundledThemeAssetSource.kt`**

```kotlin
package com.morphocore.theme.impl.registry

import android.content.res.AssetManager
import com.morphocore.theme.api.ThemeAssetSource
import java.io.IOException

// AssetManager paths omit the leading "assets/" prefix.
// Files at assets/themes/<id>/theme.json are opened as "themes/<id>/theme.json".
class BundledThemeAssetSource(private val assets: AssetManager) : ThemeAssetSource {
    override val id: String = "bundled-themes"

    override suspend fun listThemeIds(): List<String> =
        assets.list("themes")?.toList() ?: emptyList()

    override suspend fun readThemeManifest(themeId: String): String? =
        try {
            assets.open("themes/$themeId/theme.json").bufferedReader().readText()
        } catch (e: IOException) {
            null
        }
}
```

- [ ] **Step 2: Create `ThemeRegistryImplTest.kt`**

```kotlin
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
```

- [ ] **Step 3: Create `ThemeRegistryImpl.kt`**

```kotlin
package com.morphocore.theme.impl.registry

import com.morphocore.domain.Theme
import com.morphocore.theme.api.ThemeAssetSource
import com.morphocore.theme.api.ThemeRegistry
import com.morphocore.theme.impl.parsing.ThemeParseResult
import com.morphocore.theme.impl.parsing.parseTheme
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ThemeRegistryImpl(
    private val source: ThemeAssetSource,
    private val ioDispatcher: CoroutineDispatcher,
    @Suppress("UnusedPrivateMember") private val scope: CoroutineScope
) : ThemeRegistry {

    private val _themes = MutableStateFlow<List<Theme>>(emptyList())
    override val themes: StateFlow<List<Theme>> = _themes.asStateFlow()

    override suspend fun refresh() {
        withContext(ioDispatcher) {
            val loaded = mutableListOf<Theme>()
            for (themeId in source.listThemeIds()) {
                val raw = source.readThemeManifest(themeId) ?: continue
                val path = "${source.id}:themes/$themeId/theme.json"
                when (val result = parseTheme(path, raw)) {
                    is ThemeParseResult.Success -> loaded += result.theme
                    is ThemeParseResult.Failure -> { /* log and continue */ }
                }
            }
            _themes.value = loaded
        }
    }
}
```

- [ ] **Step 4: Run tests** *(requires JDK)*

```powershell
.\gradlew :theme:theme-impl:test --tests "*.ThemeRegistryImplTest"
```
Expected: 5 tests PASSED.

- [ ] **Step 5: Commit**

```powershell
git add theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/registry/
git add theme/theme-impl/src/test/kotlin/com/morphocore/theme/impl/registry/
git commit -m "feat(theme-impl): add ThemeRegistryImpl and BundledThemeAssetSource

Scans assets/themes/ for theme.json files. Partial-failure pattern: one
bad theme manifest does not prevent other themes from loading."
```

---

## Task 7: ThemeProviderImpl — TDD

**Files:**
- Create: `theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/provider/ThemePreferences.kt`
- Create: `theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/provider/ThemeProviderImpl.kt`
- Create: `theme/theme-impl/src/test/kotlin/com/morphocore/theme/impl/provider/ThemeProviderImplTest.kt`

- [ ] **Step 1: Create `ThemePreferences.kt`**

```kotlin
package com.morphocore.theme.impl.provider

import android.content.SharedPreferences

internal interface ThemePreferences {
    fun getLastThemeId(): String?
    fun saveThemeId(id: String)
}

internal class SharedPreferencesThemePrefs(
    private val prefs: SharedPreferences
) : ThemePreferences {
    override fun getLastThemeId(): String? = prefs.getString("active_theme_id", null)
    override fun saveThemeId(id: String) {
        prefs.edit().putString("active_theme_id", id).apply()
    }
}

// Test double — used only in tests
internal class FakeThemePreferences : ThemePreferences {
    var lastId: String? = null
    override fun getLastThemeId(): String? = lastId
    override fun saveThemeId(id: String) { lastId = id }
}
```

- [ ] **Step 2: Create `ThemeProviderImplTest.kt`**

```kotlin
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
        // dojo and iron both have isDefault=false
        val ironJson = fixture("iron-theme.json")
        advanceUntilIdle()
        val provider = buildProvider(
            manifests = mapOf("dojo" to dojoJson, "iron" to ironJson),
            dispatcher = dispatcher,
            scope = this
        )
        assertNotNull(provider.activeTheme.value)
    }
}
```

- [ ] **Step 3: Create `ThemeProviderImpl.kt`**

```kotlin
package com.morphocore.theme.impl.provider

import com.morphocore.domain.Theme
import com.morphocore.theme.api.ThemeProvider
import com.morphocore.theme.impl.registry.ThemeRegistryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeProviderImpl(
    private val registry: ThemeRegistryImpl,
    private val prefs: ThemePreferences
) : ThemeProvider {

    private val _activeTheme = MutableStateFlow(resolveInitialTheme())
    override val activeTheme: StateFlow<Theme> = _activeTheme.asStateFlow()

    override suspend fun setActiveTheme(themeId: String) {
        val found = registry.themes.value.find { it.id == themeId } ?: return
        _activeTheme.value = found
        prefs.saveThemeId(themeId)
    }

    private fun resolveInitialTheme(): Theme {
        val themes = registry.themes.value
        val savedId = prefs.getLastThemeId()
        if (savedId != null) {
            val saved = themes.find { it.id == savedId }
            if (saved != null) return saved
        }
        return themes.find { it.isDefault } ?: themes.first()
    }
}
```

- [ ] **Step 4: Run tests** *(requires JDK)*

```powershell
.\gradlew :theme:theme-impl:test --tests "*.ThemeProviderImplTest"
```
Expected: 6 tests PASSED.

- [ ] **Step 5: Run all theme-impl tests**

```powershell
.\gradlew :theme:theme-impl:test
```
Expected: 28 tests PASSED (17 parser + 5 registry + 6 provider).

- [ ] **Step 6: Commit**

```powershell
git add theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/provider/
git add theme/theme-impl/src/test/kotlin/com/morphocore/theme/impl/provider/
git commit -m "feat(theme-impl): add ThemeProviderImpl with preference persistence and 6 unit tests

Activates isDefault theme on first launch, restores last-selected ID from
preferences, falls back to first available theme if nothing matches."
```

---

## Task 8: core/design-system — MorphoTheme + LocalMorphoTheme

**Files:**
- Modify: `core/design-system/build.gradle.kts`
- Create: `core/design-system/src/main/kotlin/com/morphocore/designsystem/ColorExtensions.kt`
- Create: `core/design-system/src/main/kotlin/com/morphocore/designsystem/TypographyExtensions.kt`
- Create: `core/design-system/src/main/kotlin/com/morphocore/designsystem/ShapeExtensions.kt`
- Create: `core/design-system/src/main/kotlin/com/morphocore/designsystem/ResolvedMorphoTokens.kt`
- Create: `core/design-system/src/main/kotlin/com/morphocore/designsystem/LocalMorphoTheme.kt`
- Create: `core/design-system/src/main/kotlin/com/morphocore/designsystem/MorphoTheme.kt`

- [ ] **Step 1: Rewrite `core/design-system/build.gradle.kts`**

```kotlin
plugins {
    id("morphocore.compose.library")
}

android {
    namespace = "com.morphocore.core.designsystem"
}

dependencies {
    api(project(":core:domain"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
}
```

- [ ] **Step 2: Create `ColorExtensions.kt`**

```kotlin
package com.morphocore.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.morphocore.domain.MorphoColors

internal fun MorphoColors.toMaterial3ColorScheme(): ColorScheme {
    val base = if (isLightBackground(background)) lightColorScheme() else darkColorScheme()
    return base.copy(
        primary          = Color(primary),
        onPrimary        = Color(onPrimary),
        secondary        = Color(secondary),
        onSecondary      = Color(onSecondary),
        background       = Color(background),
        onBackground     = Color(onBackground),
        surface          = Color(surface),
        onSurface        = Color(onSurface),
        surfaceVariant   = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        outline          = Color(outline)
    )
}

private fun isLightBackground(argb: Long): Boolean {
    val r = ((argb shr 16) and 0xFF).toFloat() / 255f
    val g = ((argb shr 8) and 0xFF).toFloat() / 255f
    val b = (argb and 0xFF).toFloat() / 255f
    return (0.2126f * r + 0.7152f * g + 0.0722f * b) > 0.5f
}
```

- [ ] **Step 3: Create `TypographyExtensions.kt`**

```kotlin
package com.morphocore.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.morphocore.domain.MorphoTypography
import com.morphocore.domain.TextScaleEntry

internal fun MorphoTypography.toMaterial3Typography(): Typography {
    fun entry(key: String, default: TextStyle): TextStyle =
        scale[key]?.toTextStyle() ?: default
    val base = Typography()
    return Typography(
        displayLarge  = entry("displayLarge",  base.displayLarge),
        headlineLarge = entry("headlineLarge", base.headlineLarge),
        titleLarge    = entry("titleLarge",    base.titleLarge),
        bodyLarge     = entry("bodyLarge",     base.bodyLarge),
        labelLarge    = entry("labelLarge",    base.labelLarge)
    )
}

private fun TextScaleEntry.toTextStyle() = TextStyle(
    fontSize   = sizeSp.sp,
    fontWeight = FontWeight(weight),
    lineHeight = lineHeightSp.sp
)
```

- [ ] **Step 4: Create `ShapeExtensions.kt`**

```kotlin
package com.morphocore.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import com.morphocore.domain.MorphoShapes

internal fun MorphoShapes.toMaterial3Shapes() = Shapes(
    small  = RoundedCornerShape(smallDp.dp),
    medium = RoundedCornerShape(mediumDp.dp),
    large  = RoundedCornerShape(largeDp.dp)
)
```

- [ ] **Step 5: Create `ResolvedMorphoTokens.kt`**

```kotlin
package com.morphocore.designsystem

import androidx.compose.ui.graphics.Color
import com.morphocore.domain.Theme

data class ResolvedMorphoTokens(
    // Pre-resolved with primary fallback. Key = discipline id (e.g. "karate").
    val disciplineAccents: Map<String, Color>,
    // Pre-resolved. Keys: "easy", "medium", "hard".
    val difficultyColors: Map<String, Color>,
    val motionDurationShortMs: Int,
    val motionDurationMediumMs: Int,
    val motionDurationLongMs: Int,
    val activeTheme: Theme
)
```

- [ ] **Step 6: Create `LocalMorphoTheme.kt`**

```kotlin
package com.morphocore.designsystem

import androidx.compose.runtime.compositionLocalOf

val LocalMorphoTheme = compositionLocalOf<ResolvedMorphoTokens> {
    error("No MorphoTheme provided — wrap your UI in MorphoTheme { }")
}
```

- [ ] **Step 7: Create `MorphoTheme.kt`**

```kotlin
package com.morphocore.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.morphocore.domain.Theme

@Composable
fun MorphoTheme(theme: Theme, content: @Composable () -> Unit) {
    val colorScheme = theme.colors.toMaterial3ColorScheme()
    val typography  = theme.typography.toMaterial3Typography()
    val shapes      = theme.shapes.toMaterial3Shapes()
    val tokens      = theme.toResolvedMorphoTokens(fallbackColor = colorScheme.primary)

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = typography,
        shapes      = shapes
    ) {
        CompositionLocalProvider(LocalMorphoTheme provides tokens) {
            content()
        }
    }
}

private fun Theme.toResolvedMorphoTokens(fallbackColor: Color): ResolvedMorphoTokens {
    val resolved = colors.semantic.mapValues { (_, argb) -> Color(argb) }
    return ResolvedMorphoTokens(
        disciplineAccents = listOf("karate", "kungfu", "yoga", "gym", "calisthenics")
            .associateWith { id -> resolved["discipline.$id"] ?: fallbackColor },
        difficultyColors = mapOf(
            "easy"   to (resolved["difficulty.easy"]   ?: Color(0xFF4CAF50L)),
            "medium" to (resolved["difficulty.medium"] ?: Color(0xFFFF9800L)),
            "hard"   to (resolved["difficulty.hard"]   ?: Color(0xFFF44336L))
        ),
        motionDurationShortMs  = motion.durationShortMs,
        motionDurationMediumMs = motion.durationMediumMs,
        motionDurationLongMs   = motion.durationLongMs,
        activeTheme = this
    )
}
```

- [ ] **Step 8: Commit**

```powershell
git add core/design-system/
git commit -m "feat(design-system): add MorphoTheme, LocalMorphoTheme, ResolvedMorphoTokens

MorphoTheme wraps MaterialTheme with tokens derived from the active Theme domain
object. Discipline accent fallback: absent semantic key → MaterialTheme primary.
Font custom loading deferred — system fonts used for now."
```

---

## Task 9: rendering/rendering-testing — FakeSceneViewport

**Files:**
- Rewrite: `rendering/rendering-testing/build.gradle.kts`
- Delete: `rendering/rendering-testing/src/main/AndroidManifest.xml`
- Create: `rendering/rendering-api/src/main/kotlin/com/morphocore/rendering/api/LoadedModelFactory.kt`
- Create: `rendering/rendering-testing/src/main/kotlin/com/morphocore/rendering/testing/ViewportCall.kt`
- Create: `rendering/rendering-testing/src/main/kotlin/com/morphocore/rendering/testing/FakeSceneViewport.kt`
- Create: `rendering/rendering-testing/src/test/kotlin/com/morphocore/rendering/testing/FakeSceneViewportTest.kt`

- [ ] **Step 1: Rewrite `rendering/rendering-testing/build.gradle.kts`**

```kotlin
plugins { id("morphocore.kotlin.library") }
dependencies {
    implementation(project(":rendering:rendering-api"))
    implementation(project(":core:domain"))
}
```

- [ ] **Step 2: Delete the Android manifest (no longer an Android module)**

```powershell
Remove-Item "C:\Users\HP\Documents\Claude\Projects\MorphoCore\rendering\rendering-testing\src\main\AndroidManifest.xml"
```

- [ ] **Step 3: Create `LoadedModelFactory.kt` in rendering-api**

```kotlin
package com.morphocore.rendering.api

// Factory for creating LoadedModel instances in rendering implementations and test doubles.
// LoadedModel's constructor is internal; this factory is the only public creation point.
object LoadedModelFactory {
    fun create(id: String): LoadedModel = LoadedModel(id)
}
```

- [ ] **Step 4: Create `ViewportCall.kt`**

```kotlin
package com.morphocore.rendering.testing

import com.morphocore.domain.CameraPreset
import com.morphocore.domain.SceneEnvironment

sealed class ViewportCall {
    data class LoadModel(val path: String) : ViewportCall()
    data class Play(val clipName: String, val loop: Boolean) : ViewportCall()
    object Pause : ViewportCall()
    data class SeekTo(val timeSeconds: Float) : ViewportCall()
    data class SetCamera(val preset: CameraPreset, val animated: Boolean) : ViewportCall()
    data class ApplyEnvironment(val env: SceneEnvironment) : ViewportCall()
}
```

- [ ] **Step 5: Create `FakeSceneViewportTest.kt`**

```kotlin
package com.morphocore.rendering.testing

import com.morphocore.domain.CameraPreset
import com.morphocore.domain.SceneEnvironment
import com.morphocore.rendering.api.LoadedModelFactory
import com.morphocore.rendering.api.ModelLoadResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FakeSceneViewportTest {

    @Test
    fun `loadModel records call and returns success by default`() = runTest {
        val fake = FakeSceneViewport()
        val result = fake.loadModel("content/karate/kick.glb")
        assertIs<ModelLoadResult.Success>(result)
        assertEquals(1, fake.calls.size)
        assertEquals(ViewportCall.LoadModel("content/karate/kick.glb"), fake.calls.first())
    }

    @Test
    fun `loadModel returns configured failure`() = runTest {
        val fake = FakeSceneViewport(loadResult = ModelLoadResult.Failure.FileNotFound)
        val result = fake.loadModel("missing.glb")
        assertIs<ModelLoadResult.Failure.FileNotFound>(result)
    }

    @Test
    fun `play records call`() {
        val fake = FakeSceneViewport()
        fake.play("kick_loop", loop = true)
        assertEquals(ViewportCall.Play("kick_loop", true), fake.calls.first())
    }

    @Test
    fun `pause records call`() {
        val fake = FakeSceneViewport()
        fake.pause()
        assertEquals(ViewportCall.Pause, fake.calls.first())
    }

    @Test
    fun `seekTo records call`() {
        val fake = FakeSceneViewport()
        fake.seekTo(1.5f)
        assertEquals(ViewportCall.SeekTo(1.5f), fake.calls.first())
    }

    @Test
    fun `setCamera records call`() {
        val fake = FakeSceneViewport()
        val preset = CameraPreset("side")
        fake.setCamera(preset, animated = false)
        assertEquals(ViewportCall.SetCamera(preset, false), fake.calls.first())
    }

    @Test
    fun `applySceneEnvironment records call`() {
        val fake = FakeSceneViewport()
        val env = SceneEnvironment("ibl.ktx2", 0xFFFFFFFFL, 50000f)
        fake.applySceneEnvironment(env)
        assertEquals(ViewportCall.ApplyEnvironment(env), fake.calls.first())
    }

    @Test
    fun `multiple calls are recorded in order`() = runTest {
        val fake = FakeSceneViewport()
        fake.loadModel("model.glb")
        fake.play("idle", true)
        fake.pause()
        assertEquals(3, fake.calls.size)
        assertIs<ViewportCall.LoadModel>(fake.calls[0])
        assertIs<ViewportCall.Play>(fake.calls[1])
        assertIs<ViewportCall.Pause>(fake.calls[2])
    }
}
```

- [ ] **Step 6: Create `FakeSceneViewport.kt`**

```kotlin
package com.morphocore.rendering.testing

import com.morphocore.domain.CameraPreset
import com.morphocore.domain.SceneEnvironment
import com.morphocore.rendering.api.LoadedModelFactory
import com.morphocore.rendering.api.ModelLoadResult
import com.morphocore.rendering.api.SceneViewport

class FakeSceneViewport(
    private val loadResult: ModelLoadResult = ModelLoadResult.Success(LoadedModelFactory.create("fake"))
) : SceneViewport {

    val calls = mutableListOf<ViewportCall>()

    override suspend fun loadModel(modelPath: String): ModelLoadResult {
        calls += ViewportCall.LoadModel(modelPath)
        return loadResult
    }

    override fun play(clipName: String, loop: Boolean) {
        calls += ViewportCall.Play(clipName, loop)
    }

    override fun pause() {
        calls += ViewportCall.Pause
    }

    override fun seekTo(timeSeconds: Float) {
        calls += ViewportCall.SeekTo(timeSeconds)
    }

    override fun setCamera(preset: CameraPreset, animated: Boolean) {
        calls += ViewportCall.SetCamera(preset, animated)
    }

    override fun applySceneEnvironment(env: SceneEnvironment) {
        calls += ViewportCall.ApplyEnvironment(env)
    }
}
```

- [ ] **Step 7: Run FakeSceneViewport tests** *(requires JDK)*

```powershell
.\gradlew :rendering:rendering-testing:test
```
Expected: 8 tests PASSED.

- [ ] **Step 8: Commit**

```powershell
git add rendering/rendering-api/src/main/kotlin/com/morphocore/rendering/api/LoadedModelFactory.kt
git add rendering/rendering-testing/
git commit -m "feat(rendering): add FakeSceneViewport, ViewportCall, LoadedModelFactory

rendering-testing converted from Android library to pure-Kotlin module.
FakeSceneViewport records all method calls for ViewModel unit test assertions.
LoadedModelFactory exposes LoadedModel creation without requiring Android or GPU."
```

---

## Task 10: SceneViewportImpl + SceneViewportSurface

**Files:**
- Modify: `rendering/rendering-scene-view/build.gradle.kts`
- Create: `rendering/rendering-scene-view/src/main/kotlin/com/morphocore/rendering/sceneview/SceneViewportImpl.kt`
- Create: `rendering/rendering-scene-view/src/main/kotlin/com/morphocore/rendering/sceneview/SceneViewportSurface.kt`

- [ ] **Step 1: Rewrite `rendering/rendering-scene-view/build.gradle.kts`**

```kotlin
plugins { id("morphocore.compose.library") }

android {
    namespace = "com.morphocore.rendering.sceneview"
}

dependencies {
    implementation(project(":rendering:rendering-api"))
    implementation(project(":core:domain"))
    implementation(libs.sceneview)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
}
```

- [ ] **Step 2: Create `SceneViewportImpl.kt`**

```kotlin
package com.morphocore.rendering.sceneview

import com.morphocore.domain.CameraPreset
import com.morphocore.domain.SceneEnvironment
import com.morphocore.rendering.api.LoadedModelFactory
import com.morphocore.rendering.api.ModelLoadResult
import com.morphocore.rendering.api.SceneViewport
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.withTimeout
import java.io.FileNotFoundException

class SceneViewportImpl : SceneViewport {

    private var sceneView: SceneView? = null
    private var currentModelNode: ModelNode? = null

    // Called by SceneViewportSurface when the AndroidView is created.
    internal fun onViewAttached(view: SceneView) {
        sceneView = view
    }

    // Called by SceneViewportSurface when the AndroidView is released.
    internal fun onViewDetached() {
        currentModelNode = null
        sceneView = null
    }

    override suspend fun loadModel(modelPath: String): ModelLoadResult {
        val view = sceneView ?: return ModelLoadResult.Failure.FileNotFound
        return try {
            withTimeout(5_000L) {
                val node = ModelNode(view.engine)
                node.loadModelGlb(
                    context       = view.context,
                    glbFileSource = modelPath,
                    autoAnimate   = false
                )
                currentModelNode = node
                view.addChildNode(node)
                ModelLoadResult.Success(LoadedModelFactory.create(modelPath))
            }
        } catch (e: FileNotFoundException) {
            ModelLoadResult.Failure.FileNotFound
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            ModelLoadResult.Failure.Timeout
        } catch (e: OutOfMemoryError) {
            ModelLoadResult.Failure.GpuOutOfMemory
        } catch (e: Exception) {
            ModelLoadResult.Failure.ParseError(e)
        }
    }

    override fun play(clipName: String, loop: Boolean) {
        currentModelNode?.playAnimation(clipName, loop = loop)
    }

    override fun pause() {
        currentModelNode?.stopAnimation()
    }

    override fun seekTo(timeSeconds: Float) {
        currentModelNode?.animationTime = timeSeconds
    }

    override fun setCamera(preset: CameraPreset, animated: Boolean) {
        // Camera preset animation is managed by the SceneView's built-in camera controller.
        // Full implementation in Sprint 3 when camera presets are wired to UI.
    }

    override fun applySceneEnvironment(env: SceneEnvironment) {
        // IBL + directional light update applied via SceneThemeApplier in Sprint 3.
    }
}
```

- [ ] **Step 3: Create `SceneViewportSurface.kt`**

```kotlin
package com.morphocore.rendering.sceneview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.sceneview.SceneView

@Composable
fun SceneViewportSurface(
    viewport: SceneViewportImpl,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            SceneView(context).also { viewport.onViewAttached(it) }
        },
        modifier = modifier,
        onRelease = { viewport.onViewDetached() }
    )
}
```

- [ ] **Step 4: Commit**

```powershell
git add rendering/rendering-scene-view/
git commit -m "feat(rendering-scene-view): add SceneViewportImpl and SceneViewportSurface

SceneViewportImpl wraps SceneView. loadModel maps SceneView exceptions to
typed ModelLoadResult failure cases. SceneViewportSurface embeds the view
into Compose via AndroidView. Camera/IBL wiring deferred to Sprint 3."
```

---

## Task 11: SmokeTestActivity in app

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/kotlin/com/morphocore/app/SmokeTestActivity.kt`

- [ ] **Step 1: Add Compose activity dependency to `app/build.gradle.kts`**

Open `app/build.gradle.kts`. The current content is:
```kotlin
plugins { id("morphocore.android.application") }
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":content:content-api"))
    implementation(project(":content:content-impl"))
    implementation(project(":content:content-sources"))
    implementation(project(":rendering:rendering-api"))
    implementation(project(":theme:theme-api"))
}
```

Replace with:
```kotlin
plugins {
    id("morphocore.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    buildFeatures { compose = true }
    namespace = "com.morphocore.app"
}
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":content:content-api"))
    implementation(project(":content:content-impl"))
    implementation(project(":content:content-sources"))
    implementation(project(":rendering:rendering-api"))
    implementation(project(":rendering:rendering-scene-view"))
    implementation(project(":theme:theme-api"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
}
```

- [ ] **Step 2: Register SmokeTestActivity in `app/src/main/AndroidManifest.xml`**

Open the current manifest:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name=".MorphoCoreApplication"
        android:label="MorphoCore"
        android:theme="@style/Theme.AppCompat" />
</manifest>
```

Replace with:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name=".MorphoCoreApplication"
        android:label="MorphoCore"
        android:theme="@style/Theme.AppCompat">
        <activity
            android:name=".SmokeTestActivity"
            android:exported="true" />
    </application>
</manifest>
```

- [ ] **Step 3: Create `SmokeTestActivity.kt`**

```kotlin
package com.morphocore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.morphocore.rendering.sceneview.SceneViewportImpl
import com.morphocore.rendering.sceneview.SceneViewportSurface

// Manual device smoke-test. Not the launcher activity.
// Launch via: adb shell am start -n com.morphocore.app/.SmokeTestActivity
class SmokeTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewport = remember { SceneViewportImpl() }
            LaunchedEffect(viewport) {
                viewport.loadModel("content/karate/roundhouse_kick.glb")
            }
            SceneViewportSurface(
                viewport = viewport,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

- [ ] **Step 4: Commit**

```powershell
git add app/
git commit -m "feat(app): add SmokeTestActivity for manual SceneView device verification

Not the launcher. Load via adb. Hardcodes karate roundhouse kick path
which won't resolve until .glb assets are produced in content sprint."
```

---

## Task 12: Final build verification

- [ ] **Step 1: Verify all modules build** *(requires Android Studio JDK)*

```powershell
.\gradlew build
```
Expected: `BUILD SUCCESSFUL` — all 16 modules compile.

- [ ] **Step 2: Run all tests**

```powershell
.\gradlew test
```
Expected output:
```
:core:domain:test               — 16 tests PASSED  (12 Sprint1 + 4 ThemeTest)
:theme:theme-impl:test          — 28 tests PASSED  (17 parser + 5 registry + 6 provider)
:rendering:rendering-testing:test — 8 tests PASSED
BUILD SUCCESSFUL
```
Total: 52 tests, all green.

- [ ] **Step 3: Verify no feature module has impl deps**

```powershell
.\gradlew :feature:feature-browse:dependencies --configuration compileClasspath
```
Confirm `theme-impl`, `content-impl`, `rendering-scene-view` do NOT appear in the output.

- [ ] **Step 4: Final commit**

```powershell
git add -A
git commit -m "chore: Sprint 2 complete — rendering + theme system foundation

SceneViewportImpl + FakeSceneViewport + SmokeTestActivity.
ThemeManifestParser + ThemeRegistryImpl + ThemeProviderImpl.
MorphoTheme + LocalMorphoTheme Compose wrappers.
52 unit tests across domain, theme-impl, rendering-testing."
```

---

## Self-review against spec

| Spec requirement | Task |
|---|---|
| morphocore.compose.library convention plugin | Task 1 |
| Theme domain type expansion (all 8 new types) | Task 2 |
| Theme manifest schema v2 | Task 3 |
| All 4 theme fixtures (dojo, iron, studio, neon) | Task 3 |
| ThemeAssetSource interface in theme-api | Task 4 |
| ThemeManifestParser with 17 tests | Task 5 |
| ThemeRegistryImpl with 5 tests | Task 6 |
| ThemeProviderImpl + ThemePreferences with 6 tests | Task 7 |
| BundledThemeAssetSource | Task 6 |
| ResolvedMorphoTokens + LocalMorphoTheme + MorphoTheme | Task 8 |
| Discipline accent fallback → primary | Task 8 |
| LoadedModelFactory in rendering-api | Task 9 |
| rendering-testing converted to pure-Kotlin | Task 9 |
| FakeSceneViewport + ViewportCall with 8 tests | Task 9 |
| SceneViewportImpl with typed error mapping | Task 10 |
| SceneViewportSurface Composable | Task 10 |
| SmokeTestActivity registered in manifest | Task 11 |
| SceneThemeApplier | Deferred to Sprint 3 ✓ |
| Hilt DI wiring | Deferred to Sprint 3 ✓ |
