package com.morphocore.feature.detail

data class PlaybackState(
    val currentClip: String,
    val isPlaying: Boolean,
    val speedMultiplier: Float = 1f,
    val cameraPreset: String? = null
)
