package com.morphocore.content.api

sealed class ContentError {
    data class ManifestParseFailure(val path: String, val cause: Throwable) : ContentError()
    data class AssetSourceUnavailable(val sourceId: String) : ContentError()
    object NoContentFound : ContentError()
}
