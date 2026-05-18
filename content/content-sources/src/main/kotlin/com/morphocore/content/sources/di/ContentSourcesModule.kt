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
