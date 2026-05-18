# Sprint 3 Design — Hilt DI + Navigation + Browse + Movements

**Date:** 2026-05-18
**Status:** Approved

## Goal

Wire the three infrastructure layers (content, theme, rendering) into a Hilt DI graph, add type-safe Compose Navigation, and build the first two user-facing screens: the discipline browser and the movement list.

After this sprint the app launches, shows a real list of disciplines from `assets/content/`, and lets the user drill into a discipline's movement list. The detail screen (viewport) is deferred to Sprint 4.

---

## What is NOT in scope

- feature-detail (movement detail with 3D viewport + SceneThemeApplier) — Sprint 4
- feature-settings (theme picker) — Sprint 4
- Play Asset Delivery / asset packs — post-v1
- Real `.glb` model assets — content sprint

---

## Architecture overview

```
app
 ├── @HiltAndroidApp MorphoCoreApplication
 ├── MainActivity (@AndroidEntryPoint)
 │    └── MorphoTheme(activeTheme) { MorphoNavHost() }
 └── MorphoNavHost
      ├── Browse → BrowseScreen
      ├── Movements(disciplineId) → MovementsScreen
      ├── Detail(movementId) → [stub, Sprint 4]
      └── Settings → [stub, Sprint 4]

feature-browse
 └── BrowseViewModel (@HiltViewModel)
      ↓ ContentRepository.observeDisciplines()
 └── BrowseScreen → DisciplineCard × N

feature-movements
 └── MovementsViewModel (@HiltViewModel)
      ↓ ContentRepository.observeMovements(disciplineId)
 └── MovementsScreen → MovementRow × N

content-testing (new pure-Kotlin module)
 └── FakeContentRepository — test double for feature ViewModel tests

DI modules (one per impl module)
 ├── ContentModule (content-impl) — binds ContentRegistryImpl + ContentRepositoryImpl
 ├── ContentSourcesModule (content-sources) — provides BundledAssetSource
 └── ThemeModule (theme-impl) — provides ThemeRegistryImpl + ThemeProviderImpl + BundledThemeAssetSource
```

---

## Step 1 — Build config additions

### libs.versions.toml additions

```toml
[versions]
navigation        = "2.8.4"
ksp               = "2.0.21-1.0.28"
hilt-compose      = "1.2.0"

[libraries]
navigation-compose           = { module = "androidx.navigation:navigation-compose",          version.ref = "navigation" }
hilt-navigation-compose      = { module = "androidx.hilt:hilt-navigation-compose",           version.ref = "hilt-compose" }
hilt-android-testing         = { module = "com.google.dagger:hilt-android-testing",          version.ref = "hilt" }

[plugins]
ksp              = { id = "com.google.devtools.ksp",              version.ref = "ksp" }
```

### New convention plugins

**`morphocore.hilt.library.gradle.kts`** — Android library + Hilt:
```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
android {
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation(kotlin("test"))
}
tasks.withType<Test>().configureEach { useJUnitPlatform() }
```

**`morphocore.compose.feature.gradle.kts`** — Compose + Hilt + Navigation (for feature modules):
```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
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
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}
tasks.withType<Test>().configureEach { useJUnitPlatform() }
```

### build-logic/build.gradle.kts — add KSP plugin dep

Add `implementation(libs.gradlePlugin.ksp)` and the corresponding entry in `[libraries]`:
```toml
gradlePlugin-ksp = { module = "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin", version.ref = "ksp" }
```

---

## Step 2 — Hilt DI modules

### content-impl: ContentModule

File: `content/content-impl/src/main/kotlin/com/morphocore/content/impl/di/ContentModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class ContentModule {
    @Binds @Singleton
    abstract fun bindContentRegistry(impl: ContentRegistryImpl): ContentRegistry

    @Binds @Singleton
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository
}
```

`ContentRegistryImpl` and `ContentRepositoryImpl` need `@Inject constructor` annotations (and their dependencies need to be injected too — `AssetSource`, `AppDispatchers`, `CoroutineScope`).

### content-sources: ContentSourcesModule

File: `content/content-sources/src/main/kotlin/com/morphocore/content/sources/di/ContentSourcesModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ContentSourcesModule {
    @Provides @Singleton
    fun provideBundledAssetSource(@ApplicationContext ctx: Context): AssetSource =
        BundledAssetSource(ctx.assets)
}
```

### theme-impl: ThemeModule

File: `theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/di/ThemeModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ThemeModule {
    @Provides @Singleton
    fun provideBundledThemeAssetSource(@ApplicationContext ctx: Context): ThemeAssetSource =
        BundledThemeAssetSource(ctx.assets)

    @Provides @Singleton
    fun provideThemeRegistry(source: ThemeAssetSource, dispatchers: AppDispatchers,
                              @ApplicationScope scope: CoroutineScope): ThemeRegistry =
        ThemeRegistryImpl(source, dispatchers.io, scope)

    @Provides @Singleton
    fun provideThemeProvider(registry: ThemeRegistryImpl, prefs: ThemePreferences): ThemeProvider =
        ThemeProviderImpl(registry, prefs)
}
```

### core/common: AppDispatchersModule + ApplicationScope qualifier

File: `core/common/src/main/kotlin/com/morphocore/common/di/AppDispatchersModule.kt`

```kotlin
@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppDispatchersModule {
    @Provides @Singleton fun provideAppDispatchers(): AppDispatchers = AppDispatchers()

    @Provides @Singleton @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
```

---

## Step 3 — content-testing module (new)

A new pure-Kotlin module `content/content-testing` provides `FakeContentRepository` for use in feature ViewModel tests. Mirrors `rendering-testing`.

```
content/
  content-testing/
    build.gradle.kts         → morphocore.kotlin.library
    src/main/kotlin/com/morphocore/content/testing/
      FakeContentRepository.kt
```

`FakeContentRepository`:
```kotlin
class FakeContentRepository(
    private val disciplines: List<Discipline> = emptyList(),
    private val movementsByDiscipline: Map<String, List<Movement>> = emptyMap(),
    private val movementsById: Map<String, Movement> = emptyMap()
) : ContentRepository {
    override fun observeDisciplines(): Flow<List<Discipline>> = flowOf(disciplines)
    override fun observeMovements(disciplineId: String): Flow<List<Movement>> =
        flowOf(movementsByDiscipline[disciplineId] ?: emptyList())
    override suspend fun getMovement(movementId: String): Movement? = movementsById[movementId]
}
```

`settings.gradle.kts` gains: `include(":content:content-testing")`

---

## Step 4 — Navigation + MainActivity

### libs.versions.toml: add navigation + kotlinx-serialization (for @Serializable destinations)

Navigation 2.8 type-safe destinations require `kotlinx-serialization` on the navigation destinations. The `kotlin-serialization` plugin is already in `libs.versions.toml`; `navigation-compose` and `hilt-navigation-compose` libraries are added above.

### Navigation destinations (app module)

File: `app/src/main/kotlin/com/morphocore/app/navigation/AppDestinations.kt`

```kotlin
@Serializable object Browse
@Serializable data class Movements(val disciplineId: String)
@Serializable data class Detail(val movementId: String)
@Serializable object Settings
```

### MorphoNavHost

File: `app/src/main/kotlin/com/morphocore/app/navigation/MorphoNavHost.kt`

```kotlin
@Composable
fun MorphoNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController, startDestination = Browse, modifier = modifier) {
        composable<Browse> {
            BrowseScreen(
                onDisciplineSelected = { id -> navController.navigate(Movements(id)) },
                onSettingsClick = { navController.navigate(Settings) }
            )
        }
        composable<Movements> { backStackEntry ->
            val dest: Movements = backStackEntry.toRoute()
            MovementsScreen(
                disciplineId = dest.disciplineId,
                onMovementSelected = { id -> navController.navigate(Detail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<Detail> { /* Sprint 4 */ }
        composable<Settings> { /* Sprint 4 */ }
    }
}
```

### MainActivity

File: `app/src/main/kotlin/com/morphocore/app/MainActivity.kt`

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themeProvider: ThemeProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by themeProvider.activeTheme.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            MorphoTheme(theme = theme) {
                MorphoNavHost(navController = navController)
            }
        }
    }
}
```

### MorphoCoreApplication (update to @HiltAndroidApp)

```kotlin
@HiltAndroidApp
class MorphoCoreApplication : Application()
```

---

## Step 5 — feature-browse

### BrowseUiState

```kotlin
sealed class BrowseUiState {
    object Loading : BrowseUiState()
    data class Ready(val disciplines: List<Discipline>) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}
```

### BrowseViewModel

```kotlin
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val contentRepository: ContentRepository
) : ViewModel() {

    val uiState: StateFlow<BrowseUiState> = contentRepository
        .observeDisciplines()
        .map { disciplines ->
            if (disciplines.isEmpty()) BrowseUiState.Loading
            else BrowseUiState.Ready(disciplines)
        }
        .catch { e -> emit(BrowseUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseUiState.Loading)
}
```

### BrowseScreen

```kotlin
@Composable
fun BrowseScreen(
    onDisciplineSelected: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // TopAppBar with title "Disciplines" and settings icon
    // when Loading → CircularProgressIndicator
    // when Ready → LazyColumn of DisciplineCard
    // when Error → error text + retry button (calls viewModel.retry())
}
```

### DisciplineCard

```kotlin
@Composable
fun DisciplineCard(discipline: Discipline, onClick: () -> Unit) {
    // Card with discipline.displayName, discipline.description (first 80 chars),
    // colored leading bar using LocalMorphoTheme.current.disciplineAccents[discipline.id]
}
```

### BrowseViewModelTest — 5 tests

1. `uiState is Loading when repository emits empty list`
2. `uiState is Ready when repository emits disciplines`
3. `disciplines are in order emitted by repository`
4. `uiState is Error when repository throws`
5. `error message is propagated from exception`

---

## Step 6 — feature-movements

### MovementsUiState

```kotlin
sealed class MovementsUiState {
    object Loading : MovementsUiState()
    data class Ready(val disciplineName: String, val movements: List<Movement>) : MovementsUiState()
    data class Error(val message: String) : MovementsUiState()
}
```

### MovementsViewModel

```kotlin
@HiltViewModel
class MovementsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val disciplineId: String = checkNotNull(savedStateHandle["disciplineId"])

    val uiState: StateFlow<MovementsUiState> = contentRepository
        .observeMovements(disciplineId)
        .combine(contentRepository.observeDisciplines()) { movements, disciplines ->
            val discipline = disciplines.find { it.id == disciplineId }
            if (movements.isEmpty()) MovementsUiState.Loading
            else MovementsUiState.Ready(
                disciplineName = discipline?.displayName ?: disciplineId,
                movements = movements
            )
        }
        .catch { e -> emit(MovementsUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovementsUiState.Loading)
}
```

**Note on SavedStateHandle with Compose Navigation 2.8:** Hilt + Navigation 2.8 type-safe routes inject route arguments into `SavedStateHandle` automatically — no manual bundle extraction needed.

### MovementsScreen

```kotlin
@Composable
fun MovementsScreen(
    disciplineId: String,
    onMovementSelected: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MovementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // TopAppBar with back arrow + discipline name
    // LazyColumn of MovementRow
}
```

### MovementRow

```kotlin
@Composable
fun MovementRow(movement: Movement, onClick: () -> Unit) {
    // ListItem with movement.displayName, movement.difficulty badge,
    // muscle group chips (first 3), duration text
}
```

### MovementsViewModelTest — 5 tests

1. `uiState is Loading when repository emits empty movements`
2. `uiState is Ready when movements are loaded`
3. `disciplineName is taken from discipline display name`
4. `disciplineId fallback used when discipline not found`
5. `uiState is Error when repository throws`

---

## Step 7 — app module wiring

Update `app/build.gradle.kts`:
- Switch to `morphocore.android.application` + Hilt + KSP + Compose plugins
- Add feature modules + design-system + navigation deps
- Add `theme-impl` + `content-impl` for DI modules to be visible at app scope

Update `AndroidManifest.xml`:
- Replace `SmokeTestActivity` intent-filter with `MainActivity` as launcher
- Keep `SmokeTestActivity` with `android:exported="true"` for manual testing

---

## File map

```
# Build config
gradle/libs.versions.toml                                                  MODIFY
build-logic/build.gradle.kts                                               MODIFY
build-logic/src/main/kotlin/morphocore.hilt.library.gradle.kts             CREATE
build-logic/src/main/kotlin/morphocore.compose.feature.gradle.kts          CREATE

# DI wiring
core/common/src/main/kotlin/com/morphocore/common/di/AppDispatchersModule.kt  CREATE
content/content-impl/src/main/kotlin/com/morphocore/content/impl/di/ContentModule.kt  CREATE
content/content-sources/src/main/kotlin/com/morphocore/content/sources/di/ContentSourcesModule.kt  CREATE
theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/di/ThemeModule.kt  CREATE

# content-testing (new module)
settings.gradle.kts                                                        MODIFY
content/content-testing/build.gradle.kts                                   CREATE
content/content-testing/src/main/kotlin/com/morphocore/content/testing/FakeContentRepository.kt  CREATE

# Navigation
app/src/main/kotlin/com/morphocore/app/navigation/AppDestinations.kt      CREATE
app/src/main/kotlin/com/morphocore/app/navigation/MorphoNavHost.kt         CREATE
app/src/main/kotlin/com/morphocore/app/MainActivity.kt                     CREATE
app/src/main/kotlin/com/morphocore/app/MorphoCoreApplication.kt            MODIFY (@HiltAndroidApp)
app/build.gradle.kts                                                       MODIFY
app/src/main/AndroidManifest.xml                                           MODIFY

# feature-browse
feature/feature-browse/build.gradle.kts                                    MODIFY
feature/feature-browse/src/main/kotlin/com/morphocore/feature/browse/BrowseUiState.kt  CREATE
feature/feature-browse/src/main/kotlin/com/morphocore/feature/browse/BrowseViewModel.kt  CREATE
feature/feature-browse/src/main/kotlin/com/morphocore/feature/browse/ui/DisciplineCard.kt  CREATE
feature/feature-browse/src/main/kotlin/com/morphocore/feature/browse/ui/BrowseScreen.kt  CREATE
feature/feature-browse/src/test/kotlin/com/morphocore/feature/browse/BrowseViewModelTest.kt  CREATE

# feature-movements
feature/feature-movements/build.gradle.kts                                 MODIFY
feature/feature-movements/src/main/kotlin/com/morphocore/feature/movements/MovementsUiState.kt  CREATE
feature/feature-movements/src/main/kotlin/com/morphocore/feature/movements/MovementsViewModel.kt  CREATE
feature/feature-movements/src/main/kotlin/com/morphocore/feature/movements/ui/MovementRow.kt  CREATE
feature/feature-movements/src/main/kotlin/com/morphocore/feature/movements/ui/MovementsScreen.kt  CREATE
feature/feature-movements/src/test/kotlin/com/morphocore/feature/movements/MovementsViewModelTest.kt  CREATE
```

---

## Testing strategy

All ViewModel tests are pure JUnit 5 + coroutines-test. No Robolectric, no Android runtime. `FakeContentRepository` is the test double. `BrowseViewModelTest` and `MovementsViewModelTest` use `StandardTestDispatcher` + `advanceUntilIdle()` (same pattern as content + theme tests).

Compose UI tests (requiring Android runtime) are deferred — they add significant complexity and the ViewModel tests give high confidence at low cost.

---

## Spec corrections (from self-review against actual Sprint 1 domain types)

The `Discipline` type is `data class Discipline(id, name, iconPath, movementIds)` — no `description`, no `accentColorToken`. The `Movement` type is `data class Movement(id, disciplineId, name, modelPath, defaultClip, clips, muscles, difficulty, tags, cameraPreset, prerequisites, commonMistakes)` — no `durationSeconds`. Use `name` everywhere (not `displayName`).

**DisciplineCard correction** — no description line, no accent color bar (field doesn't exist):
```kotlin
@Composable
fun DisciplineCard(discipline: Discipline, onClick: () -> Unit) {
    // ElevatedCard with discipline.name as title,
    // movement count badge (discipline.movementIds.size)
}
```

**MovementRow correction** — no duration:
```kotlin
@Composable
fun MovementRow(movement: Movement, onClick: () -> Unit) {
    // ListItem with movement.name, difficulty badge, first 3 muscle group chips
}
```

**ContentModule correction** — `ContentRegistryImpl` takes `List<AssetSource>` and `ContentRepositoryImpl` takes `ContentRegistryImpl` (concrete type):
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ContentModule {
    @Provides @Singleton
    fun provideContentRegistryImpl(
        source: AssetSource,                   // single BundledAssetSource from ContentSourcesModule
        dispatchers: AppDispatchers,
        @ApplicationScope scope: CoroutineScope
    ): ContentRegistryImpl =
        ContentRegistryImpl(listOf(source), dispatchers.io, scope)

    @Provides @Singleton
    fun provideContentRegistry(impl: ContentRegistryImpl): ContentRegistry = impl

    @Provides @Singleton
    fun provideContentRepository(registry: ContentRegistryImpl): ContentRepository =
        ContentRepositoryImpl(registry)
}
```

**ThemeModule correction** — must also provide `ThemePreferences`:
```kotlin
@Provides @Singleton
fun provideThemePreferences(@ApplicationContext ctx: Context): ThemePreferences =
    SharedPreferencesThemePrefs(
        ctx.getSharedPreferences("morphocore_theme", Context.MODE_PRIVATE)
    )
```

**MovementsUiState.Ready correction** — use `disciplineName` from `discipline.name`:
```kotlin
data class Ready(val disciplineName: String, val movements: List<Movement>) : MovementsUiState()
```
(no change — just confirming `discipline.name` is the correct field)

---

## Constraints

- **No Java/Gradle execution.** All code is written from spec. Tests require Android Studio JDK.
- **KSP over KAPT.** Kotlin 2.0.21 deprecates KAPT; KSP is the correct Hilt processor.
- **No NavController in ViewModel.** Navigation events are Lambda callbacks from Screen → NavHost. ViewModels are navigation-unaware.
- **`@Inject constructor`** added to `ContentRegistryImpl`, `ContentRepositoryImpl`, `BundledAssetSource` — Hilt needs it to build the object graph.
