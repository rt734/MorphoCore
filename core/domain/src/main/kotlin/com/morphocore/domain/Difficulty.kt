package com.morphocore.domain

enum class Difficulty {
    BEGINNER, INTERMEDIATE, ADVANCED;

    companion object {
        fun fromString(value: String): Difficulty = valueOf(value.uppercase())
    }
}
