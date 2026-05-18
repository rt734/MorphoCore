# MorphoCore Sprint 2 — Rendering Integration + Theme System Design Spec
**Date:** 2026-05-18
**Sprint:** Steps 4–5 of the v1 Roadmap (Rendering Integration · Theme System Foundation)

---

## 1. Goal

Implement the rendering abstraction layer and the theme system foundation. By the end of this sprint:

- `rendering/rendering-scene-view` contains a working `SceneViewportImpl` wrapping SceneView/Filament
- `rendering/rendering-testing` contains `FakeSceneViewport` for ViewModel unit tests (no GPU required)
- `app` contains a `SmokeTestActivity` for manual device verification
- `core/domain` has an expanded `Theme` type carrying all token data
- `theme/theme-impl` parses theme manifests, discovers themes at runtime, and exposes the active theme via `ThemeProviderImpl`
- `core/design-system` contains `MorphoTheme { }` and `LocalMorphoTheme` wiring Compose to the active theme

The `SceneThemeApplier` (the bridge between theme changes and Filament IBL) is **deferred to Sprint 3** (detail screen). This sprint delivers the two halves of that bridge independently and verifies each works in isolation.

---

## 2. Scope

### In scope

**Domain expansion:**
- Replace `Theme(id, name, iblPath, colorTokens)` with the full typed type tree
- Replace `ColorTokens` with `MorphoColors` (full palette + semantic map)
- Add `MorphoTypography`, `MorphoShapes`, `MorphoMotion`, `SceneConfig`, `DirectLightConfig`, `GroundPlaneConfig`, `PostProcessingConfig`
- Remove `ColorTokens` from `core/domain` (no other code references it yet)
- Keep `SceneEnvironment` in `rendering/rendering-api` — it is the lightweight call-site type derived from `SceneConfig` at application time

**Schema and fixtures:**
- Rewrite `schemas/theme-manifest.schema.json` to match the full doc schema
- Rewrite `schemas/fixtures/dojo-theme.json` and `schemas/fixtures/iron-theme.json` to full fidelity
- Add `schemas/fixtures/studio-theme.json` and `schemas/fixtures/neon-theme.json`
- Copy all four theme fixtures to `theme/theme-impl/src/test/resources/fixtures/`

**`theme/theme-impl`:**
- `ThemeManifestParser` — JSON → `ParseResult<Theme>`
- `ThemeRegistryImpl` — scans `assets/themes/`, implements `ThemeRegistry`
- `ThemeProviderImpl` — holds active theme, persists selection, implements `ThemeProvider`
- `FakeThemeAssetSource` — test double for `ThemeRegistryImpl` tests
- Unit tests for all three (JVM only — no device, no Compose)

**`core/design-system`:**
- Add Compose BOM + Material3 + Activity-Compose dependencies
- `ResolvedMorphoTokens` — holds fully resolved Compose-ready token values
- `LocalMorphoTheme` — `CompositionLocal<ResolvedMorphoTokens>`
- `MorphoTheme { }` — Composable that takes a `Theme`, builds `MaterialTheme` tokens, provides `LocalMorphoTheme`
- Discipline accent fallback: absent semantic token → `MaterialTheme.colorScheme.primary`

**`rendering/rendering-scene-view`:**
- Add `morphocore.compose.library` convention plugin (enables Compose compiler)
- Add SceneView 2.2.1 dependency
- `SceneViewportImpl` — implements `SceneViewport`, wraps SceneView internals
- `SceneViewportSurface` — Composable that embeds the SceneView `AndroidView`

**`rendering/rendering-testing`:**
- `FakeSceneViewport` — pure-Kotlin test double implementing `SceneViewport`
- `ViewportCall` — sealed class recording all method calls

**`app`:**
- `SmokeTestActivity` — manual device verification of SceneView initialization
- Register in `AndroidManifest.xml`

**Build config:**
- Add `sceneview`, `compose-bom`, `compose-ui`, `compose-material3`, `compose-activity`, `datastore-preferences` to `libs.versions.toml`
- Add `morphocore.compose.library` convention plugin to `build-logic`

### Out of scope

- `SceneThemeApplier` (Sprint 3 — requires the detail screen to exist)
- Playing actual animations in the smoke-test (model path is a placeholder — `.glb` files don't exist yet)
- Content asset production (`.glb`, `.ktx2` files)
- Any feature screen (browse, movements, detail, settings)
- Hilt DI wiring in `app` (deferred to Sprint 3 when feature screens are wired together)

---

## 3. Domain Type Expansion (`core/domain`)

### 3.1 Removed types

- `ColorTokens` — superseded by `MorphoColors`

### 3.2 Replaced/expanded `Theme`

```kotlin
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

### 3.3 New supporting types

```kotlin
// All color values are ARGB Long (0xFFRRGGBB).
// Hex strings from manifests are converted to ARGB Long during parsing.
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
    // Key format: "discipline.<id>" or "difficulty.<level>"
    val semantic: Map<String, Long>
)

data class MorphoTypography(
    val displayFontPath: String?,   // relative to theme folder, e.g. "fonts/NotoSerifJP-Bold.ttf"
    val bodyFontPath: String?,
    val labelFontPath: String?,
    val scale: Map<String, TextScaleEntry>  // key = "displayLarge", "bodyLarge", etc.
)

data class TextScaleEntry(val sizeSp: Int, val weight: Int, val lineHeightSp: Int)

data class MorphoShapes(
    val smallDp: Float,
    val mediumDp: Float,
    val largeDp: Float
)

data class MorphoMotion(
    val durationShortMs: Int,
    val durationMediumMs: Int,
    val durationLongMs: Int,
    val easingStandard: String   // e.g. "cubicBezier(0.2, 0.0, 0.0, 1.0)"
)

data class SceneConfig(
    val skyboxPath: String?,               // null = no visible skybox
    val iblEnvironmentPath: String,        // required
    val iblIntensity: Float,
    val directLight: DirectLightConfig,
    val ambientIntensity: Float,
    val groundPlane: GroundPlaneConfig,
    val postProcessing: PostProcessingConfig
)

data class DirectLightConfig(
    val color: Long,              // ARGB
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
    val toneMapping: String       // "ACES", "LINEAR", "FILMIC"
)
```

### 3.4 Retained types

`SceneEnvironment` and `CameraPreset` in `rendering/rendering-api` are unchanged. `SceneEnvironment` remains the lightweight rendering command type. Application code derives it from `SceneConfig` at the point of calling `applySceneEnvironment()`.

---

## 4. Theme Manifest Schema (updated)

The schema is rewritten to match `05-theme-system.md` exactly. Key changes from Sprint 1:

- `schemaVersion` is now an integer `1` (not the string `"1.0"`)
- `themeId` → `id`
- `themeName` → `displayName`
- Top-level `iblPath` and `directionalLight` are replaced by the nested `scene` object
- Top-level `colorTokens` (7 values) is replaced by `colors` with full palette + semantic map
- `typography`, `shapes`, `motion`, `icons` sections added

### 4.1 Updated fixture structure (all four themes)

Each fixture covers: `schemaVersion`, `id`, `displayName`, `description`, `isDefault`, `colors` (full palette + semantic), `typography`, `shapes`, `motion`, `scene` (skybox, IBL, lights, post-processing).

| Fixture | isDefault | Character |
|---|---|---|
| `dojo-theme.json` | false | Warm wood, martial arts aesthetic |
| `iron-theme.json` | false | Dark charcoal, hard rim light |
| `studio-theme.json` | **true** | Clean white, neutral IBL |
| `neon-theme.json` | false | Deep black, cyan/magenta bloom |

`studio` is the default because it's the most broadly legible across disciplines.

---

## 5. `theme/theme-impl` — Parser, Registry, Provider

### 5.1 `ThemeManifestParser`

Internal function `parseTheme(path: String, jsonString: String): ParseResult<Theme>`.

- DTOs are internal `@Serializable` data classes (same pattern as `ManifestParser` in Sprint 1)
- Hex color strings (`#RRGGBB`) converted to ARGB Long via `hexToArgbLong(hex: String): Long`
- `schemaVersion` guard: only version `1` accepted; any other value → `ParseResult.Failure`
- Required fields: `id`, `displayName`, `colors.primary`, `colors.background`, `scene.iblEnvironmentPath`
- Optional fields default: `description = ""`, `isDefault = false`, `semantic = emptyMap()`, all motion/shape values have sensible fallbacks
- Exception in parsing → `ParseResult.Failure` (never thrown)

```kotlin
// ParseResult mirrors the content layer pattern
internal sealed class ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>()
    data class Failure(val error: ThemeError) : ParseResult<Nothing>()
}

sealed class ThemeError {
    data class ManifestParseFailure(val path: String, val cause: Throwable) : ThemeError()
    object NoThemesFound : ThemeError()
}
```

### 5.2 `ThemeRegistryImpl`

```kotlin
class ThemeRegistryImpl(
    private val assetSource: ThemeAssetSource,
    private val ioDispatcher: CoroutineDispatcher,
    private val scope: CoroutineScope
) : ThemeRegistry {
    private val _themes = MutableStateFlow<List<Theme>>(emptyList())
    override val themes: StateFlow<List<Theme>> = _themes.asStateFlow()

    override suspend fun refresh() { ... }
}
```

`ThemeAssetSource` interface (in `theme/theme-api` alongside `ThemeRegistry`):
```kotlin
interface ThemeAssetSource {
    val id: String
    suspend fun listThemeIds(): List<String>
    suspend fun readThemeManifest(themeId: String): String?
}
```

`BundledThemeAssetSource` in `theme/theme-impl` implements `ThemeAssetSource` using `AssetManager`, reading from `assets/themes/<id>/theme.json`.

`ThemeRegistryImpl.refresh()` scans all theme IDs, parses each, collects successes. Partial parse failures are logged but don't block other themes. Unlike content registry, theme registry does NOT have a `Loading/Ready/Error` state — it exposes `StateFlow<List<Theme>>` directly (empty list until refresh completes). This simplifies the API since themes are not lazy-loaded and the list is small.

### 5.3 `ThemeProviderImpl`

```kotlin
class ThemeProviderImpl(
    private val registry: ThemeRegistryImpl,
    private val prefs: SharedPreferences,
    private val ioDispatcher: CoroutineDispatcher
) : ThemeProvider {
    private val _activeTheme = MutableStateFlow<Theme>(/* placeholder — set in init */ ...)
    override val activeTheme: StateFlow<Theme> = _activeTheme.asStateFlow()
    ...
}
```

- On construction, reads `prefs.getString("active_theme_id", null)`. If a stored ID exists and is found in the registry, activates that theme; otherwise activates the `isDefault = true` theme; otherwise activates `registry.themes.value.firstOrNull()`.
- `setActiveTheme(id)` finds the theme in `registry.themes.value`, updates `_activeTheme`, and persists the new ID to `prefs`.
- Requires `registry.refresh()` to have been called before construction (or immediately after) so `registry.themes.value` is non-empty.

---

## 6. `core/design-system` — Compose Tokens

### 6.1 Dependencies added

```kotlin
// core/design-system/build.gradle.kts
plugins { id("morphocore.compose.library") }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    api(project(":core:domain"))
}
```

### 6.2 `ResolvedMorphoTokens`

```kotlin
data class ResolvedMorphoTokens(
    val disciplineAccents: Map<String, Color>,   // resolved with primary fallback
    val difficultyColors: Map<String, Color>,
    val motionDurationShortMs: Int,
    val motionDurationMediumMs: Int,
    val motionDurationLongMs: Int,
    val activeTheme: Theme                        // full theme for consumers that need raw values
)
```

### 6.3 `LocalMorphoTheme`

```kotlin
val LocalMorphoTheme = compositionLocalOf<ResolvedMorphoTokens> {
    error("No MorphoTheme provided")
}
```

### 6.4 `MorphoTheme { }`

```kotlin
@Composable
fun MorphoTheme(theme: Theme, content: @Composable () -> Unit) {
    val colorScheme = theme.colors.toMaterial3ColorScheme()
    val typography = theme.typography.toMaterial3Typography()
    val shapes = theme.shapes.toMaterial3Shapes()
    val morphoTokens = theme.toResolvedMorphoTokens(colorScheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes
    ) {
        CompositionLocalProvider(LocalMorphoTheme provides morphoTokens) {
            content()
        }
    }
}
```

Extension functions `toMaterial3ColorScheme()`, `toMaterial3Typography()`, `toMaterial3Shapes()` are private to this file. `toResolvedMorphoTokens()` applies the semantic fallback logic.

**Semantic fallback for discipline accents:**
```kotlin
// If "discipline.karate" absent from semantic map, use primary
fun MorphoColors.resolveSemanticColor(key: String, fallback: Color): Color =
    semantic[key]?.let { Color(it) } ?: fallback
```

---

## 7. `rendering/rendering-scene-view` — SceneViewportImpl

### 7.1 Dependencies

```kotlin
// rendering/rendering-scene-view/build.gradle.kts
plugins { id("morphocore.compose.library") }
dependencies {
    implementation(project(":rendering:rendering-api"))
    implementation(project(":core:domain"))
    implementation(libs.sceneview)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
}
```

### 7.2 `SceneViewportImpl`

`SceneViewportImpl` is NOT a Composable itself — it is a class that manages scene state and exposes the `SceneViewport` interface. A companion `SceneViewportSurface` Composable (in the same file) creates an `AndroidView` hosting the SceneView `SceneView`.

```kotlin
class SceneViewportImpl : SceneViewport {
    // Internal: SceneView engine reference, set when the Composable surface is attached
    private var sceneView: io.github.sceneview.SceneView? = null
    private var currentModel: io.github.sceneview.node.ModelNode? = null

    // Called by SceneViewportSurface when the view is created
    internal fun onViewAttached(view: io.github.sceneview.SceneView) { sceneView = view }
    internal fun onViewDetached() { sceneView = null; currentModel = null }

    override suspend fun loadModel(modelPath: String): ModelLoadResult { ... }
    override fun play(clipName: String, loop: Boolean) { ... }
    override fun pause() { ... }
    override fun seekTo(timeSeconds: Float) { ... }
    override fun setCamera(preset: CameraPreset, animated: Boolean) { ... }
    override fun applySceneEnvironment(env: SceneEnvironment) { ... }
}
```

`loadModel` maps exceptions:
- `FileNotFoundException` → `ModelLoadResult.Failure.FileNotFound`
- SceneView parse / decode exception → `ModelLoadResult.Failure.ParseError`
- `OutOfMemoryError` → `ModelLoadResult.Failure.GpuOutOfMemory`
- Timeout (5 second timeout via `withTimeout`) → `ModelLoadResult.Failure.Timeout`

### 7.3 `SceneViewportSurface` Composable

```kotlin
@Composable
fun SceneViewportSurface(
    viewport: SceneViewportImpl,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            io.github.sceneview.SceneView(context).also { viewport.onViewAttached(it) }
        },
        modifier = modifier,
        onRelease = { viewport.onViewDetached() }
    )
}
```

Feature modules reference `SceneViewport` (the api interface) for the ViewModel. The detail screen Composable (Sprint 3) will use `SceneViewportSurface` to embed the surface.

---

## 8. `rendering/rendering-testing` — FakeSceneViewport

```kotlin
class FakeSceneViewport(
    private val loadResult: ModelLoadResult = ModelLoadResult.Success(
        LoadedModel("fake-model")  // cannot construct LoadedModel externally — use reflection or test factory
    )
) : SceneViewport {
    val calls = mutableListOf<ViewportCall>()

    override suspend fun loadModel(modelPath: String): ModelLoadResult {
        calls += ViewportCall.LoadModel(modelPath)
        return loadResult
    }
    override fun play(clipName: String, loop: Boolean) { calls += ViewportCall.Play(clipName, loop) }
    override fun pause() { calls += ViewportCall.Pause }
    override fun seekTo(timeSeconds: Float) { calls += ViewportCall.SeekTo(timeSeconds) }
    override fun setCamera(preset: CameraPreset, animated: Boolean) {
        calls += ViewportCall.SetCamera(preset, animated)
    }
    override fun applySceneEnvironment(env: SceneEnvironment) {
        calls += ViewportCall.ApplyEnvironment(env)
    }
}
```

**Note on `LoadedModel` construction:** `LoadedModel` has an `internal` constructor in `rendering-api`. `FakeSceneViewport` is in `rendering-testing`, which is a separate module — `internal` doesn't span modules. Solution: add a `testLoadedModel(id: String)` factory function in `rendering-testing` that uses a `@VisibleForTesting` annotation or a companion in `LoadedModel`. Simplest approach: add `internal constructor` + expose a test helper in the same module via `@TestOnly`.

Better solution: move `LoadedModel` to be created by a factory in `rendering-api`:

```kotlin
// In rendering-api
object LoadedModelFactory {
    // Only for use by rendering implementations and test doubles
    fun create(id: String): LoadedModel = LoadedModel(id)
}
```

This is package-private-ish via convention but avoids reflection.

---

## 9. `app` — SmokeTestActivity

```kotlin
class SmokeTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewport = remember { SceneViewportImpl() }
            LaunchedEffect(viewport) {
                viewport.loadModel("content/karate/roundhouse_kick.glb")
            }
            SceneViewportSurface(viewport = viewport, modifier = Modifier.fillMaxSize())
        }
    }
}
```

Registered in `AndroidManifest.xml` under `<activity android:name=".SmokeTestActivity" />`. Not the launcher. Launched manually via ADB for device verification: `adb shell am start -n com.morphocore.app/.SmokeTestActivity`.

---

## 10. Build config additions

### `libs.versions.toml` additions

```toml
[versions]
# ... existing ...
sceneview      = "2.2.1"
compose-bom    = "2024.09.03"
datastore      = "1.1.1"

[libraries]
# ... existing ...
compose-bom              = { module = "androidx.compose:compose-bom",                    version.ref = "compose-bom" }
compose-ui               = { module = "androidx.compose.ui:ui" }
compose-material3        = { module = "androidx.compose.material3:material3" }
compose-activity         = { module = "androidx.activity:activity-compose",              version = "1.9.2" }
sceneview                = { module = "io.github.sceneview:sceneview",                   version.ref = "sceneview" }
datastore-preferences    = { module = "androidx.datastore:datastore-preferences",        version.ref = "datastore" }
```

Note: `compose-ui` and `compose-material3` have no `version.ref` because they are resolved via the Compose BOM.

### New `morphocore.compose.library` convention plugin

```kotlin
// build-logic/src/main/kotlin/morphocore.compose.library.gradle.kts
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
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

---

## 11. Testing Plan

| Module | Type | What is verified |
|---|---|---|
| `core/domain` | Unit | Expanded `Theme` constructs correctly; `MorphoColors.resolveSemanticColor` fallback works |
| `theme/theme-impl` | Unit | `parseTheme` parses all 4 fixtures correctly |
| `theme/theme-impl` | Unit | `parseTheme` returns `Failure` on schemaVersion ≠ 1 |
| `theme/theme-impl` | Unit | `parseTheme` returns `Failure` on malformed JSON |
| `theme/theme-impl` | Unit | `parseTheme` applies defaults for missing optional fields |
| `theme/theme-impl` | Coroutines | `ThemeRegistryImpl` populates `themes` after `refresh()` |
| `theme/theme-impl` | Coroutines | `ThemeRegistryImpl` handles partial failures without dropping other themes |
| `theme/theme-impl` | Coroutines | `ThemeProviderImpl` activates `isDefault` theme by default |
| `theme/theme-impl` | Coroutines | `ThemeProviderImpl.setActiveTheme` updates the flow and persists |
| `rendering/rendering-testing` | Unit | `FakeSceneViewport` records calls in order |
| `rendering/rendering-testing` | Unit | `FakeSceneViewport.loadModel` returns configured result |
| `SceneViewportImpl` | Manual (device) | Smoke-test Activity initializes SceneView without crashing |

All JVM tests run without a device. `SceneViewportImpl` and `SmokeTestActivity` require Android Studio + real device.

---

## 12. Definition of Done

1. `./gradlew build` succeeds for all 16 modules (once Android Studio is installed)
2. `./gradlew test` passes all new unit tests for `core/domain`, `theme/theme-impl`, `rendering/rendering-testing`
3. `SceneViewportImpl` compiles (GPU verification deferred to device smoke-test)
4. `MorphoTheme { }` and `LocalMorphoTheme` compile in `core/design-system`
5. All 4 theme fixtures parse successfully in `ThemeManifestParser` tests
6. `FakeSceneViewport` records calls correctly (verified by unit tests)
7. Clean git history: each logical component committed separately

---

## 13. What Comes Next (Sprint 3)

Sprint 3 (Steps 6–7) picks up from here and adds:
- Discipline browser screen + ViewModel (`feature/feature-browse`)
- Movement list screen + ViewModel (`feature/feature-movements`)
- Movement detail screen with `SceneViewportSurface` embedded + ViewModel (`feature/feature-detail`)
- `SceneThemeApplier` wired into the detail screen — the bridge between `ThemeProvider` and `SceneViewportImpl`
- Hilt DI wiring in `app` (all three registries, providers, repositories injected)
