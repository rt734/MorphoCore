package com.morphocore.content.api

import kotlinx.coroutines.flow.StateFlow

interface ContentRegistry {
    val state: StateFlow<RegistryState>
    suspend fun refresh()
}
