package com.morphocore.domain

data class Discipline(
    val id: String,
    val name: String,
    val iconPath: String?,
    val movementIds: List<String>
)
