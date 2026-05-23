package com.morphocore.app.navigation

import kotlinx.serialization.Serializable

@Serializable
object Browse

@Serializable
data class Movements(val disciplineId: String, val initialTag: String? = null)

@Serializable
data class Detail(val movementId: String)

@Serializable
object Settings
