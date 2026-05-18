package com.morphocore.content.api

sealed class RegistryState {
    object Loading : RegistryState()
    object Ready : RegistryState()
    data class Error(val cause: ContentError) : RegistryState()
}
