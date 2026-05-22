package com.morphocore.domain

data class Movement(
    val id: String,
    val disciplineId: String,
    val name: String,
    val description: String = "",
    val modelPath: String,
    val defaultClip: String,
    val clips: List<AnimationClip>,
    val muscles: List<MuscleGroup>,
    val difficulty: Difficulty,
    val tags: List<String>,
    val cameraPreset: String?,
    val prerequisites: List<String>,
    val commonMistakes: List<String>
) {
    init {
        require(id.startsWith("$disciplineId.")) {
            "Movement id '$id' must start with disciplineId '$disciplineId.'"
        }
    }
}
