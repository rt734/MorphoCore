package com.morphocore.rendering.api

sealed class ModelLoadResult {
    data class Success(val model: LoadedModel) : ModelLoadResult()
    sealed class Failure : ModelLoadResult() {
        object FileNotFound : Failure()
        data class ParseError(val cause: Throwable) : Failure()
        object GpuOutOfMemory : Failure()
        object Timeout : Failure()
    }
}

// Constructor is internal so only the rendering implementation can create instances.
class LoadedModel internal constructor(val id: String)
