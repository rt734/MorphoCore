package com.morphocore.domain

data class AnimationClip(
    val name: String,
    val durationSeconds: Float,
    val fps: Int
) {
    init {
        require(durationSeconds > 0f) { "durationSeconds must be positive, was $durationSeconds" }
        require(fps in 1..120) { "fps must be in 1..120, was $fps" }
    }
}
