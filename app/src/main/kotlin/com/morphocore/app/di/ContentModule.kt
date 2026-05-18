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
