package com.morphocore.domain

sealed class MuscleGroup {
    object Quadriceps  : MuscleGroup()
    object Hamstrings  : MuscleGroup()
    object Glutes      : MuscleGroup()
    object Core        : MuscleGroup()
    object Shoulders   : MuscleGroup()
    object Back        : MuscleGroup()
    object Chest       : MuscleGroup()
    object Calves      : MuscleGroup()
    object HipFlexors  : MuscleGroup()
    data class Unknown(val raw: String) : MuscleGroup()

    companion object {
        fun fromString(value: String): MuscleGroup = when (value.lowercase()) {
            "quadriceps"  -> Quadriceps
            "hamstrings"  -> Hamstrings
            "glutes"      -> Glutes
            "core"        -> Core
            "shoulders"   -> Shoulders
            "back"        -> Back
            "chest"       -> Chest
            "calves"      -> Calves
            "hip_flexors", "hip-flexors" -> HipFlexors
            else          -> Unknown(value)
        }
    }
}
