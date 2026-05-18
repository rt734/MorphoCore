# Sprint 3 — Hilt DI + Navigation + Browse + Movements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire the content and theme layers into a Hilt DI graph, add type-safe Compose Navigation, and build the discipline browser and movement list screens.

**Architecture:** `ContentModule` (in app/di) and `ThemeModule` (in theme-impl/di) bind each layer's impl to its interface via Hilt `@Provides`. `MainActivity` hosts `MorphoNavHost` wrapped in `MorphoTheme`. `BrowseViewModel` and `MovementsViewModel` inject `ContentRepository` and expose a sealed `UiState` flow. `FakeContentRepository` in a new `content-testing` module allows pure-JVM ViewModel tests with no Android runtime.

**Tech Stack:** Kotlin 2.0.21 · Hilt 2.52 · KSP 2.0.21-1.0.28 · Compose Navigation 2.8.4 · Compose BOM 2024.09.03 · Material3 · kotlinx.coroutines-test · JUnit 5

---

## Actual domain types (verified from source)

```kotlin
// core/domain
data class Discipline(val id: String, val name: String, val iconPath: String?, val movementIds: List<String>)
data class Movement(val id: String, val disciplineId: String, val name: String, val modelPath: String,
    val defaultClip: String, val clips: List<AnimationClip>, val muscles: List<MuscleGroup>,
    val difficulty: Difficulty, val tags: List<String>, val cameraPreset: String?,
    val prerequisites: List<String>, val commonMistakes: List<String>)
enum class Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }
sealed class MuscleGroup { object Quadriceps, Hamstrings, Glutes, Core, Shoulders, Back, Chest, Calves, HipFlexors; data class Unknown(val raw: String) }
data class AppDispatchers(val io: CoroutineDispatcher, val default: CoroutineDispatcher, val main: CoroutineDispatcher)

// content/content-api
interface ContentRepository {
    fun observeDisciplines(): Flow<List<Discipline>>
    fun observeMovements(disciplineId: String): Flow<List<Movement>>
    suspend fun getMovement(movementId: String): Movement?
}

// content/content-impl
class ContentRegistryImpl(sources: List<AssetSource>, ioDispatcher: CoroutineDispatcher, scope: CoroutineScope)
class ContentRepositoryImpl(registry: ContentRegistryImpl) : ContentRepository
// ContentRepositoryImpl.observeDisciplines() sorts by name
// ContentRepositoryImpl.observeMovements() filters by disciplineId, sorts by difficulty then name

// theme/theme-impl
class ThemeRegistryImpl(source: ThemeAssetSource, ioDispatcher: CoroutineDispatcher, scope: CoroutineScope)
class ThemeProviderImpl(registry: ThemeRegistryImpl, prefs: ThemePreferences)
internal interface ThemePreferences { fun getLastThemeId(): String?; fun saveThemeId(id: String) }
internal class SharedPreferencesThemePrefs(prefs: SharedPreferences) : ThemePreferences
```

---

## File map

```
# Build config
gradle/libs.versions.toml                                                         MODIFY
build-logic/build.gradle.kts                                                      MODIFY
build-logic/src/main/kotlin/morphocore.compose.feature.gradle.kts                CREATE
settings.gradle.kts                                                               MODIFY

# DI modules
app/src/main/kotlin/com/morphocore/app/di/AppDispatchersModule.kt                CREATE
app/src/main/kotlin/com/morphocore/app/di/ContentModule.kt                       CREATE
content/content-sources/build.gradle.kts                                          MODIFY
content/content-sources/src/main/kotlin/com/morphocore/content/sources/di/ContentSourcesModule.kt  CREATE
theme/theme-impl/build.gradle.kts                                                 MODIFY
theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/di/ThemeModule.kt     CREATE

# content-testing
content/content-testing/build.gradle.kts                                          CREATE
content/content-testing/src/main/kotlin/com/morphocore/content/testing/FakeContentRepository.kt  CREATE

# App + Navigation
app/src/main/kotlin/com/morphocore/app/MorphoCoreApplication.kt                  MODIFY (@HiltAndroidApp)
app/src/main/kotlin/com/morphocore/app/navigation/AppDestinations.kt             CREATE
app/src/main/kotlin/com/morphocore/app/navigation/MorphoNavHost.kt               CREATE
app/src/main/kotlin/com/morphocore/app/MainActivity.kt                           CREATE
app/build.gradle.kts                                                              MODIFY
app/src/main/AndroidManifest.xml                                                  MODIFY

# feature-browse
feature/feature-browse/build.gradle.kts                                           MODIFY
feature/feature-browse/src/main/kotlin/com/morphocore/feature/browse/BrowseUiState.kt  CREATE
feature/feature-browse/src/main/kotlin/com/morphocore/feature/browse/BrowseViewModel.kt  CREATE
feature/feature-browse/src/main/kotlin/com/morphocore/feature/browse/ui/DisciplineCard.kt  CREATE
feature/feature-browse/src/main/kotlin/com/morphocore/feature/browse/ui/BrowseScreen.kt  CREATE
feature/feature-browse/src/test/kotlin/com/morphocore/feature/browse/BrowseViewModelTest.kt  CREATE

# feature-movements
feature/feature-movements/build.gradle.kts                                        MODIFY
feature/feature-movements/src/main/kotlin/com/morphocore/feature/movements/MovementsUiState.kt  CREATE
feature/feature-movements/src/main/kotlin/com/morphocore/feature/movements/MovementsViewModel.kt  CREATE
feature/feature-movements/src/main/kotlin/com/morphocore/feature/movements/ui/MovementRow.kt  CREATE
feature/feature-movements/src/main/kotlin/com/morphocore/feature/movements/ui/MovementsScreen.kt  CREATE
feature/feature-movements/src/test/kotlin/com/morphocore/feature/movements/MovementsViewModelTest.kt  CREATE
```

---

## Task 1: Build config — libs.versions.toml + morphocore.compose.feature plugin

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build-logic/build.gradle.kts`
- Create: `build-logic/src/main/kotlin/morphocore.compose.feature.gradle.kts`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Add new entries to `gradle/libs.versions.toml`**

In `[versions]`, after existing entries, add:
```toml
ksp            = "2.0.21-1.0.28"
navigation     = "2.8.4"
lifecycle      = "2.8.7"
hilt-compose   = "1.2.0"
```

In `[libraries]`, after existing entries, add:
```toml
gradlePlugin-ksp             = { module = "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin", version.ref = "ksp" }
navigation-compose           = { module = "androidx.navigation:navigation-compose",              version.ref = "navigation" }
hilt-navigation-compose      = { module = "androidx.hilt:hilt-navigation-compose",              version.ref = "hilt-compose" }
lifecycle-viewmodel-compose  = { module = "androidx.lifecycle:lifecycle-viewmodel-compose",     version.ref = "lifecycle" }
lifecycle-runtime-compose    = { module = "androidx.lifecycle:lifecycle-runtime-compose",       version.ref = "lifecycle" }
```

In `[plugins]`, after existing entries, add:
```toml
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: Add KSP plugin dep to `build-logic/build.gradle.kts`**

Open `build-logic/build.gradle.kts`. Add `implementation(libs.gradlePlugin.ksp)` to the `dependencies` block:
```kotlin
plugins {
    `kotlin-dsl`
}
dependencies {
    implementation(libs.gradlePlugin.android)
    implementation(libs.gradlePlugin.kotlin)
    implementation(libs.gradlePlugin.ksp)
}
```

- [ ] **Step 3: Create `build-logic/src/main/kotlin/morphocore.compose.feature.gradle.kts`**

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
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

- [ ] **Step 4: Add `content-testing` to `settings.gradle.kts`**

Open `settings.gradle.kts`. After `include(":content:content-sources")` add:
```kotlin
include(":content:content-testing")
```

- [ ] **Step 5: Commit**

```powershell
cd "C:\Users\HP\Documents\Claude\Projects\MorphoCore"
git add gradle/libs.versions.toml build-logic/ settings.gradle.kts
git commit -m "chore: add KSP + Navigation + Lifecycle deps and morphocore.compose.feature plugin"
```

---

## Task 2: DI infrastructure — AppDispatchersModule + ContentSourcesModule

**Files:**
- Create: `app/src/main/kotlin/com/morphocore/app/di/AppDispatchersModule.kt`
- Modify: `content/content-sources/build.gradle.kts`
- Create: `content/content-sources/src/main/kotlin/com/morphocore/content/sources/di/ContentSourcesModule.kt`

- [ ] **Step 1: Create `app/src/main/kotlin/com/morphocore/app/di/AppDispatchersModule.kt`**

Create directory first:
```powershell
New-Item -ItemType Directory -Force "C:\Users\HP\Documents\Claude\Projects\MorphoCore\app\src\main\kotlin\com\morphocore\app\di"
```

Content:
```kotlin
package com.morphocore.app.di

import com.morphocore.common.AppDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppDispatchersModule {
    @Provides
    @Singleton
    fun provideAppDispatchers(): AppDispatchers = AppDispatchers()
}
```

- [ ] **Step 2: Update `content/content-sources/build.gradle.kts`**

Current content:
```kotlin
plugins { id("morphocore.android.library") }
android { namespace = "com.morphocore.content.sources" }
dependencies {
    implementation(project(":content:content-api"))
    implementation(project(":core:common"))
}
```

Replace with:
```kotlin
plugins {
    id("morphocore.android.library")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.morphocore.content.sources"
}

dependencies {
    implementation(project(":content:content-api"))
    implementation(project(":core:common"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

- [ ] **Step 3: Create `content/content-sources/src/main/kotlin/com/morphocore/content/sources/di/ContentSourcesModule.kt`**

Create directory:
```powershell
New-Item -ItemType Directory -Force "C:\Users\HP\Documents\Claude\Projects\MorphoCore\content\content-sources\src\main\kotlin\com\morphocore\content\sources\di"
```

Content:
```kotlin
package com.morphocore.content.sources.di

import android.content.Context
import com.morphocore.content.api.AssetSource
import com.morphocore.content.sources.BundledAssetSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ContentSourcesModule {
    @Provides
    @Singleton
    fun provideBundledAssetSource(@ApplicationContext ctx: Context): AssetSource =
        BundledAssetSource(ctx.assets)
}
```

- [ ] **Step 4: Commit**

```powershell
git add app/src/ content/content-sources/
git commit -m "feat(di): add AppDispatchersModule and ContentSourcesModule

AppDispatchersModule provides AppDispatchers singleton.
ContentSourcesModule provides BundledAssetSource from app assets."
```

---

## Task 3: ThemeModule in theme-impl

**Files:**
- Modify: `theme/theme-impl/build.gradle.kts`
- Create: `theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/di/ThemeModule.kt`

- [ ] **Step 1: Update `theme/theme-impl/build.gradle.kts`**

Current content:
```kotlin
plugins {
    id("morphocore.android.library")
    alias(libs.plugins.kotlin.serialization)
}
android { namespace = "com.morphocore.theme.impl" }
dependencies {
    implementation(project(":theme:theme-api"))
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
```

Replace with:
```kotlin
plugins {
    id("morphocore.android.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
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
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

- [ ] **Step 2: Create `theme/theme-impl/src/main/kotlin/com/morphocore/theme/impl/di/ThemeModule.kt`**

Create directory:
```powershell
New-Item -ItemType Directory -Force "C:\Users\HP\Documents\Claude\Projects\MorphoCore\theme\theme-impl\src\main\kotlin\com\morphocore\theme\impl\di"
```

Content:
```kotlin
package com.morphocore.theme.impl.di

import android.content.Context
import com.morphocore.common.AppDispatchers
import com.morphocore.theme.api.ThemeAssetSource
import com.morphocore.theme.api.ThemeProvider
import com.morphocore.theme.api.ThemeRegistry
import com.morphocore.theme.impl.provider.SharedPreferencesThemePrefs
import com.morphocore.theme.impl.provider.ThemePreferences
import com.morphocore.theme.impl.provider.ThemeProviderImpl
import com.morphocore.theme.impl.registry.BundledThemeAssetSource
import com.morphocore.theme.impl.registry.ThemeRegistryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ThemeModule {

    @Provides
    @Singleton
    fun provideBundledThemeAssetSource(@ApplicationContext ctx: Context): ThemeAssetSource =
        BundledThemeAssetSource(ctx.assets)

    @Provides
    @Singleton
    fun provideThemePreferences(@ApplicationContext ctx: Context): ThemePreferences =
        SharedPreferencesThemePrefs(
            ctx.getSharedPreferences("morphocore_theme", Context.MODE_PRIVATE)
        )

    @Provides
    @Singleton
    fun provideThemeRegistryImpl(
        source: ThemeAssetSource,
        dispatchers: AppDispatchers
    ): ThemeRegistryImpl =
        ThemeRegistryImpl(
            source = source,
            ioDispatcher = dispatchers.io,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )

    @Provides
    @Singleton
    fun provideThemeRegistry(impl: ThemeRegistryImpl): ThemeRegistry = impl

    @Provides
    @Singleton
    fun provideThemeProvider(
        registry: ThemeRegistryImpl,
        prefs: ThemePreferences
    ): ThemeProvider = ThemeProviderImpl(registry, prefs)
}
```

- [ ] **Step 3: Commit**

```powershell
git add theme/theme-impl/
git commit -m "feat(di): add ThemeModule — binds ThemeRegistry, ThemeProvider, ThemePreferences"
```

---

## Task 4: ContentModule (app-level) + content-testing + FakeContentRepository

**Files:**
- Create: `app/src/main/kotlin/com/morphocore/app/di/ContentModule.kt`
- Create: `content/content-testing/build.gradle.kts`
- Create: `content/content-testing/src/main/kotlin/com/morphocore/content/testing/FakeContentRepository.kt`

- [ ] **Step 1: Create `app/src/main/kotlin/com/morphocore/app/di/ContentModule.kt`**

```kotlin
package com.morphocore.app.di

import com.morphocore.common.AppDispatchers
import com.morphocore.content.api.AssetSource
import com.morphocore.content.api.ContentRegistry
import com.morphocore.content.api.ContentRepository
import com.morphocore.content.impl.registry.ContentRegistryImpl
import com.morphocore.content.impl.repository.ContentRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ContentModule {

    @Provides
    @Singleton
    fun provideContentRegistryImpl(
        source: AssetSource,
        dispatchers: AppDispatchers
    ): ContentRegistryImpl =
        ContentRegistryImpl(
            sources = listOf(source),
            ioDispatcher = dispatchers.io,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )

    @Provides
    @Singleton
    fun provideContentRegistry(impl: ContentRegistryImpl): ContentRegistry = impl

    @Provides
    @Singleton
    fun provideContentRepository(registry: ContentRegistryImpl): ContentRepository =
        ContentRepositoryImpl(registry)
}
```

- [ ] **Step 2: Create `content/content-testing/build.gradle.kts`**

```kotlin
plugins { id("morphocore.kotlin.library") }

dependencies {
    implementation(project(":content:content-api"))
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.core)
}
```

- [ ] **Step 3: Create `content/content-testing/src/main/kotlin/com/morphocore/content/testing/FakeContentRepository.kt`**

Create directory:
```powershell
New-Item -ItemType Directory -Force "C:\Users\HP\Documents\Claude\Projects\MorphoCore\content\content-testing\src\main\kotlin\com\morphocore\content\testing"
```

Content:
```kotlin
package com.morphocore.content.testing

import com.morphocore.content.api.ContentRepository
import com.morphocore.domain.Discipline
import com.morphocore.domain.Movement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class FakeContentRepository(
    private val disciplines: List<Discipline> = emptyList(),
    private val movementsByDiscipline: Map<String, List<Movement>> = emptyMap(),
    private val movementsById: Map<String, Movement> = emptyMap(),
    private val throwOnDisciplines: Exception? = null,
    private val throwOnMovements: Exception? = null
) : ContentRepository {

    override fun observeDisciplines(): Flow<List<Discipline>> =
        if (throwOnDisciplines != null) flow { throw throwOnDisciplines }
        else flowOf(disciplines)

    override fun observeMovements(disciplineId: String): Flow<List<Movement>> =
        if (throwOnMovements != null) flow { throw throwOnMovements }
        else flowOf(movementsByDiscipline[disciplineId] ?: emptyList())

    override suspend fun getMovement(movementId: String): Movement? = movementsById[movementId]
}
```

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/kotlin/com/morphocore/app/di/ContentModule.kt
git add content/content-testing/
git commit -m "feat(di): add ContentModule; add content-testing module with FakeContentRepository

ContentModule binds ContentRegistryImpl and ContentRepositoryImpl.
FakeContentRepository enables pure-JVM ViewModel tests with no Android runtime."
```

---

## Task 5: Navigation + MainActivity + @HiltAndroidApp

**Files:**
- Modify: `app/src/main/kotlin/com/morphocore/app/MorphoCoreApplication.kt`
- Create: `app/src/main/kotlin/com/morphocore/app/navigation/AppDestinations.kt`
- Create: `app/src/main/kotlin/com/morphocore/app/navigation/MorphoNavHost.kt`
- Create: `app/src/main/kotlin/com/morphocore/app/MainActivity.kt`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Update `app/build.gradle.kts`**

Replace the entire file with:
```kotlin
plugins {
    id("morphocore.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.morphocore.app"
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:design-system"))
    implementation(project(":content:content-api"))
    implementation(project(":content:content-impl"))
    implementation(project(":content:content-sources"))
    implementation(project(":rendering:rendering-api"))
    implementation(project(":rendering:rendering-scene-view"))
    implementation(project(":theme:theme-api"))
    implementation(project(":theme:theme-impl"))
    implementation(project(":feature:feature-browse"))
    implementation(project(":feature:feature-movements"))
    implementation(project(":feature:feature-detail"))
    implementation(project(":feature:feature-settings"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation("androidx.appcompat:appcompat:1.7.0")
}
```

- [ ] **Step 2: Update `app/src/main/kotlin/com/morphocore/app/MorphoCoreApplication.kt`**

```kotlin
package com.morphocore.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MorphoCoreApplication : Application()
```

- [ ] **Step 3: Create `app/src/main/kotlin/com/morphocore/app/navigation/AppDestinations.kt`**

Create directory:
```powershell
New-Item -ItemType Directory -Force "C:\Users\HP\Documents\Claude\Projects\MorphoCore\app\src\main\kotlin\com\morphocore\app\navigation"
```

Content:
```kotlin
package com.morphocore.app.navigation

import kotlinx.serialization.Serializable

@Serializable
object Browse

@Serializable
data class Movements(val disciplineId: String)

@Serializable
data class Detail(val movementId: String)

@Serializable
object Settings
```

- [ ] **Step 4: Create `app/src/main/kotlin/com/morphocore/app/navigation/MorphoNavHost.kt`**

```kotlin
package com.morphocore.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.morphocore.feature.browse.ui.BrowseScreen
import com.morphocore.feature.movements.ui.MovementsScreen

@Composable
fun MorphoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Browse,
        modifier = modifier
    ) {
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
        composable<Detail> {
            // Sprint 4
        }
        composable<Settings> {
            // Sprint 4
        }
    }
}
```

- [ ] **Step 5: Create `app/src/main/kotlin/com/morphocore/app/MainActivity.kt`**

```kotlin
package com.morphocore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.morphocore.app.navigation.MorphoNavHost
import com.morphocore.designsystem.MorphoTheme
import com.morphocore.theme.api.ThemeProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeProvider: ThemeProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            themeProvider.refresh()
        }
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

**Note:** `ThemeProvider.refresh()` doesn't exist on the interface — instead, startup refresh should happen via the `ThemeRegistryImpl`. Add a startup call for both registries:

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeProvider: ThemeProvider
    @Inject lateinit var themeRegistry: ThemeRegistry
    @Inject lateinit var contentRegistry: ContentRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            themeRegistry.refresh()
            contentRegistry.refresh()
        }
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

Add the needed imports:
```kotlin
import androidx.lifecycle.lifecycleScope
import com.morphocore.content.api.ContentRegistry
import com.morphocore.theme.api.ThemeRegistry
import kotlinx.coroutines.launch
```

- [ ] **Step 6: Update `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name=".MorphoCoreApplication"
        android:label="MorphoCore"
        android:theme="@style/Theme.AppCompat">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <activity
            android:name=".SmokeTestActivity"
            android:exported="true" />
    </application>
</manifest>
```

- [ ] **Step 7: Commit**

```powershell
git add app/
git commit -m "feat(app): add MainActivity, MorphoNavHost, type-safe navigation destinations

@HiltAndroidApp on MorphoCoreApplication. MainActivity refreshes content +
theme registries on startup and wraps UI in MorphoTheme. NavHost stubs
Detail and Settings destinations (Sprint 4)."
```

---

## Task 6: feature-browse — BrowseViewModel + tests

**Files:**
- Modify: `feature/feature-browse/build.gradle.kts`
- Create: `feature/feature-browse/src/main/kotlin/com/morphocore/feature/browse/BrowseUiState.kt`
- Create: `feature/feature-browse/src/main/kotlin/com/morphocore/feature/browse/BrowseViewModel.kt`
- Create: `feature/feature-browse/src/test/kotlin/com/morphocore/feature/browse/BrowseViewModelTest.kt`

- [ ] **Step 1: Update `feature/feature-browse/build.gradle.kts`**

```kotlin
plugins {
    id("morphocore.compose.feature")
}

android {
    namespace = "com.morphocore.feature.browse"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:design-system"))
    implementation(project(":content:content-api"))
    testImplementation(project(":content:content-testing"))
}
```

- [ ] **Step 2: Create `BrowseUiState.kt`**

Create directory:
```powershell
New-Item -ItemType Directory -Force "C:\Users\HP\Documents\Claude\Projects\MorphoCore\feature\feature-browse\src\main\kotlin\com\morphocore\feature\browse"
```

```kotlin
package com.morphocore.feature.browse

import com.morphocore.domain.Discipline

sealed class BrowseUiState {
    object Loading : BrowseUiState()
    data class Ready(val disciplines: List<Discipline>) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}
```

- [ ] **Step 3: Create `BrowseViewModel.kt`**

```kotlin
package com.morphocore.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morphocore.content.api.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

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
        .catch { e -> emit(BrowseUiState.Error(e.message ?: "Failed to load disciplines")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BrowseUiState.Loading
        )
}
```

- [ ] **Step 4: Create `BrowseViewModelTest.kt`**

Create directory:
```powershell
New-Item -ItemType Directory -Force "C:\Users\HP\Documents\Claude\Projects\MorphoCore\feature\feature-browse\src\test\kotlin\com\morphocore\feature\browse"
```

```kotlin
package com.morphocore.feature.browse

import com.morphocore.content.testing.FakeContentRepository
import com.morphocore.domain.Discipline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class BrowseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun discipline(id: String, name: String) =
        Discipline(id = id, name = name, iconPath = null, movementIds = emptyList())

    @Test
    fun `uiState is Loading when repository emits empty list`() = runTest {
        val vm = BrowseViewModel(FakeContentRepository())
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertIs<BrowseUiState.Loading>(vm.uiState.value)
    }

    @Test
    fun `uiState is Ready when repository emits disciplines`() = runTest {
        val repo = FakeContentRepository(disciplines = listOf(discipline("karate", "Karate")))
        val vm = BrowseViewModel(repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertIs<BrowseUiState.Ready>(vm.uiState.value)
    }

    @Test
    fun `Ready state contains all disciplines from repository`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate"), discipline("yoga", "Yoga"))
        )
        val vm = BrowseViewModel(repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals(2, state.disciplines.size)
    }

    @Test
    fun `disciplines are in order emitted by repository`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("yoga", "Yoga"), discipline("karate", "Karate"))
        )
        val vm = BrowseViewModel(repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as BrowseUiState.Ready
        assertEquals("yoga", state.disciplines.first().id)
    }

    @Test
    fun `uiState is Error when repository throws`() = runTest {
        val repo = FakeContentRepository(throwOnDisciplines = RuntimeException("load failed"))
        val vm = BrowseViewModel(repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = assertIs<BrowseUiState.Error>(vm.uiState.value)
        assertEquals("load failed", state.message)
    }
}
```

- [ ] **Step 5: Commit**

```powershell
git add feature/feature-browse/
git commit -m "feat(feature-browse): add BrowseViewModel with Loading/Ready/Error states and 5 unit tests"
```

---

## Task 7: feature-browse — BrowseScreen + DisciplineCard

**Files:**
- Create: `feature/feature-browse/src/main/kotlin/com/morphocore/feature/browse/ui/DisciplineCard.kt`
- Create: `feature/feature-browse/src/main/kotlin/com/morphocore/feature/browse/ui/BrowseScreen.kt`

- [ ] **Step 1: Create `DisciplineCard.kt`**

Create directory:
```powershell
New-Item -ItemType Directory -Force "C:\Users\HP\Documents\Claude\Projects\MorphoCore\feature\feature-browse\src\main\kotlin\com\morphocore\feature\browse\ui"
```

```kotlin
package com.morphocore.feature.browse.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morphocore.domain.Discipline

@Composable
fun DisciplineCard(discipline: Discipline, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = discipline.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${discipline.movementIds.size} movements",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

- [ ] **Step 2: Create `BrowseScreen.kt`**

```kotlin
package com.morphocore.feature.browse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morphocore.feature.browse.BrowseUiState
import com.morphocore.feature.browse.BrowseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onDisciplineSelected: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Disciplines") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is BrowseUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is BrowseUiState.Ready -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.disciplines, key = { it.id }) { discipline ->
                        DisciplineCard(
                            discipline = discipline,
                            onClick = { onDisciplineSelected(discipline.id) }
                        )
                    }
                }
            }
            is BrowseUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${state.message}",
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```powershell
git add feature/feature-browse/src/main/kotlin/com/morphocore/feature/browse/ui/
git commit -m "feat(feature-browse): add BrowseScreen and DisciplineCard composables"
```

---

## Task 8: feature-movements — MovementsViewModel + tests

**Files:**
- Modify: `feature/feature-movements/build.gradle.kts`
- Create: `feature/feature-movements/src/main/kotlin/com/morphocore/feature/movements/MovementsUiState.kt`
- Create: `feature/feature-movements/src/main/kotlin/com/morphocore/feature/movements/MovementsViewModel.kt`
- Create: `feature/feature-movements/src/test/kotlin/com/morphocore/feature/movements/MovementsViewModelTest.kt`

- [ ] **Step 1: Update `feature/feature-movements/build.gradle.kts`**

```kotlin
plugins {
    id("morphocore.compose.feature")
}

android {
    namespace = "com.morphocore.feature.movements"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:design-system"))
    implementation(project(":content:content-api"))
    testImplementation(project(":content:content-testing"))
}
```

- [ ] **Step 2: Create `MovementsUiState.kt`**

Create directory:
```powershell
New-Item -ItemType Directory -Force "C:\Users\HP\Documents\Claude\Projects\MorphoCore\feature\feature-movements\src\main\kotlin\com\morphocore\feature\movements"
```

```kotlin
package com.morphocore.feature.movements

import com.morphocore.domain.Movement

sealed class MovementsUiState {
    object Loading : MovementsUiState()
    data class Ready(val disciplineName: String, val movements: List<Movement>) : MovementsUiState()
    data class Error(val message: String) : MovementsUiState()
}
```

- [ ] **Step 3: Create `MovementsViewModel.kt`**

```kotlin
package com.morphocore.feature.movements

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morphocore.content.api.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MovementsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val disciplineId: String = checkNotNull(savedStateHandle["disciplineId"])

    val uiState: StateFlow<MovementsUiState> = combine(
        contentRepository.observeMovements(disciplineId),
        contentRepository.observeDisciplines()
    ) { movements, disciplines ->
        if (movements.isEmpty()) {
            MovementsUiState.Loading
        } else {
            val disciplineName = disciplines.find { it.id == disciplineId }?.name ?: disciplineId
            MovementsUiState.Ready(disciplineName = disciplineName, movements = movements)
        }
    }
        .catch { e -> emit(MovementsUiState.Error(e.message ?: "Failed to load movements")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MovementsUiState.Loading
        )
}
```

- [ ] **Step 4: Create `MovementsViewModelTest.kt`**

Create directory:
```powershell
New-Item -ItemType Directory -Force "C:\Users\HP\Documents\Claude\Projects\MorphoCore\feature\feature-movements\src\test\kotlin\com\morphocore\feature\movements"
```

```kotlin
package com.morphocore.feature.movements

import androidx.lifecycle.SavedStateHandle
import com.morphocore.content.testing.FakeContentRepository
import com.morphocore.domain.Difficulty
import com.morphocore.domain.Discipline
import com.morphocore.domain.Movement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class MovementsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun discipline(id: String, name: String) =
        Discipline(id = id, name = name, iconPath = null, movementIds = emptyList())

    private fun movement(disciplineId: String, name: String) =
        Movement(
            id = "$disciplineId.${name.lowercase().replace(" ", "_")}",
            disciplineId = disciplineId,
            name = name,
            modelPath = "models/$disciplineId/$name.glb",
            defaultClip = "idle",
            clips = emptyList(),
            muscles = emptyList(),
            difficulty = Difficulty.BEGINNER,
            tags = emptyList(),
            cameraPreset = null,
            prerequisites = emptyList(),
            commonMistakes = emptyList()
        )

    private fun savedState(disciplineId: String) =
        SavedStateHandle(mapOf("disciplineId" to disciplineId))

    @Test
    fun `uiState is Loading when movements list is empty`() = runTest {
        val vm = MovementsViewModel(savedState("karate"), FakeContentRepository())
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertIs<MovementsUiState.Loading>(vm.uiState.value)
    }

    @Test
    fun `uiState is Ready when movements are loaded`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(movement("karate", "roundhouse kick")))
        )
        val vm = MovementsViewModel(savedState("karate"), repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertIs<MovementsUiState.Ready>(vm.uiState.value)
    }

    @Test
    fun `disciplineName is taken from discipline display name`() = runTest {
        val repo = FakeContentRepository(
            disciplines = listOf(discipline("karate", "Karate")),
            movementsByDiscipline = mapOf("karate" to listOf(movement("karate", "front kick")))
        )
        val vm = MovementsViewModel(savedState("karate"), repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as MovementsUiState.Ready
        assertEquals("Karate", state.disciplineName)
    }

    @Test
    fun `disciplineId used as fallback name when discipline not found`() = runTest {
        val repo = FakeContentRepository(
            movementsByDiscipline = mapOf("karate" to listOf(movement("karate", "kick")))
        )
        val vm = MovementsViewModel(savedState("karate"), repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        val state = vm.uiState.value as MovementsUiState.Ready
        assertEquals("karate", state.disciplineName)
    }

    @Test
    fun `uiState is Error when repository throws`() = runTest {
        val repo = FakeContentRepository(throwOnMovements = RuntimeException("db error"))
        val vm = MovementsViewModel(savedState("karate"), repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertIs<MovementsUiState.Error>(vm.uiState.value)
    }
}
```

- [ ] **Step 5: Commit**

```powershell
git add feature/feature-movements/
git commit -m "feat(feature-movements): add MovementsViewModel with SavedStateHandle injection and 5 unit tests"
```

---

## Task 9: feature-movements — MovementsScreen + MovementRow

**Files:**
- Create: `feature/feature-movements/src/main/kotlin/com/morphocore/feature/movements/ui/MovementRow.kt`
- Create: `feature/feature-movements/src/main/kotlin/com/morphocore/feature/movements/ui/MovementsScreen.kt`

- [ ] **Step 1: Create `MovementRow.kt`**

Create directory:
```powershell
New-Item -ItemType Directory -Force "C:\Users\HP\Documents\Claude\Projects\MorphoCore\feature\feature-movements\src\main\kotlin\com\morphocore\feature\movements\ui"
```

```kotlin
package com.morphocore.feature.movements.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morphocore.domain.Difficulty
import com.morphocore.domain.Movement

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovementRow(movement: Movement, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = movement.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            DifficultyBadge(movement.difficulty)
        }
        if (movement.muscles.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                movement.muscles.take(3).forEach { muscle ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = muscle::class.simpleName ?: muscle.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    val label = when (difficulty) {
        Difficulty.BEGINNER -> "Beginner"
        Difficulty.INTERMEDIATE -> "Intermediate"
        Difficulty.ADVANCED -> "Advanced"
    }
    val color = when (difficulty) {
        Difficulty.BEGINNER -> MaterialTheme.colorScheme.secondary
        Difficulty.INTERMEDIATE -> MaterialTheme.colorScheme.tertiary
        Difficulty.ADVANCED -> MaterialTheme.colorScheme.error
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}
```

- [ ] **Step 2: Create `MovementsScreen.kt`**

```kotlin
package com.morphocore.feature.movements.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morphocore.feature.movements.MovementsUiState
import com.morphocore.feature.movements.MovementsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsScreen(
    disciplineId: String,
    onMovementSelected: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MovementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val state = uiState) {
                        is MovementsUiState.Ready -> state.disciplineName
                        else -> disciplineId
                    }
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is MovementsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is MovementsUiState.Ready -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    items(state.movements, key = { it.id }) { movement ->
                        MovementRow(
                            movement = movement,
                            onClick = { onMovementSelected(movement.id) }
                        )
                    }
                }
            }
            is MovementsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: ${state.message}")
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```powershell
git add feature/feature-movements/src/main/kotlin/com/morphocore/feature/movements/ui/
git commit -m "feat(feature-movements): add MovementsScreen and MovementRow composables"
```

---

## Task 10: Final integration — feature module stubs + push branch

**Files:**
- Modify: `feature/feature-detail/build.gradle.kts`
- Modify: `feature/feature-settings/build.gradle.kts`

These modules need at least a minimal namespace so the app module can depend on them without build errors.

- [ ] **Step 1: Update `feature/feature-detail/build.gradle.kts`**

```kotlin
plugins {
    id("morphocore.compose.feature")
}

android {
    namespace = "com.morphocore.feature.detail"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":rendering:rendering-api"))
    implementation(project(":content:content-api"))
    implementation(project(":core:design-system"))
}
```

- [ ] **Step 2: Update `feature/feature-settings/build.gradle.kts`**

```kotlin
plugins {
    id("morphocore.compose.feature")
}

android {
    namespace = "com.morphocore.feature.settings"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":theme:theme-api"))
    implementation(project(":core:design-system"))
}
```

- [ ] **Step 3: Final commit**

```powershell
git add feature/feature-detail/ feature/feature-settings/
git commit -m "chore: update feature-detail and feature-settings to morphocore.compose.feature plugin

Minimal stubs so app module can depend on them. Full implementation Sprint 4."
```

- [ ] **Step 4: Push branch** *(requires remote)*

```powershell
git push -u origin sprint-3-browse-movements-navigation
```

---

## Self-review against spec

| Spec requirement | Task |
|---|---|
| KSP added to libs.versions.toml + build-logic | Task 1 |
| navigation + hilt-compose + lifecycle libs added | Task 1 |
| morphocore.compose.feature convention plugin | Task 1 |
| content-testing added to settings.gradle | Task 1 |
| AppDispatchersModule provides AppDispatchers | Task 2 |
| ContentSourcesModule in content-sources | Task 2 |
| ThemeModule — ThemeRegistry + ThemeProvider + ThemePreferences | Task 3 |
| ContentModule (app-level) — ContentRegistryImpl + ContentRepository | Task 4 |
| FakeContentRepository with throwOnDisciplines/Movements | Task 4 |
| @HiltAndroidApp on MorphoCoreApplication | Task 5 |
| Type-safe @Serializable nav destinations | Task 5 |
| MorphoNavHost with Browse + Movements composable routes | Task 5 |
| MainActivity refreshes content + theme registries on start | Task 5 |
| BrowseViewModel: Loading/Ready/Error from observeDisciplines() | Task 6 |
| BrowseViewModelTest: 5 tests via FakeContentRepository | Task 6 |
| BrowseScreen: LazyColumn + TopAppBar + error/loading states | Task 7 |
| DisciplineCard: name + movement count | Task 7 |
| MovementsViewModel: combine movements + disciplines for name | Task 8 |
| MovementsViewModel: disciplineId from SavedStateHandle | Task 8 |
| MovementsViewModelTest: 5 tests | Task 8 |
| MovementsScreen: LazyColumn + TopAppBar with back + title | Task 9 |
| MovementRow: name + difficulty badge + muscle chips | Task 9 |
| feature-detail + feature-settings stub build files | Task 10 |
