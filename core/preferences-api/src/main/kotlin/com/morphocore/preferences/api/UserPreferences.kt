package com.morphocore.preferences.api

interface UserPreferences {
    fun getDefaultSpeed(): Float
    fun setDefaultSpeed(speed: Float)
    fun getDefaultCamera(): String?
    fun setDefaultCamera(preset: String?)
}
