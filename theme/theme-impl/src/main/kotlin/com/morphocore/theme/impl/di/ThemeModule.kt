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
