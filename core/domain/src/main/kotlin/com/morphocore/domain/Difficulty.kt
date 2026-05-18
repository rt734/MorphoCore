package com.morphocore.domain

enum class Difficulty {
    BEGINNER, INTERMEDIATE, ADVANCED;

    companion object {
        fun fromString(value: String): Difficulty =
            entries.firstOrNull { it.name == value.uppercase() }
                ?: throw IllegalArgumentException(
                    "Unknown difficulty '$value'. Expected one of: ${entries.map { it.name }}"
                )
    }
}
