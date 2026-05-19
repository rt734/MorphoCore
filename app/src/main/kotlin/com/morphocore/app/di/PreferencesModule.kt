package com.morphocore.app.di

import android.content.Context
import android.content.SharedPreferences
import com.morphocore.preferences.api.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private class SharedPreferencesUserPrefs(private val prefs: SharedPreferences) : UserPreferences {
    override fun getDefaultSpeed(): Float = prefs.getFloat("default_speed", 1f)
    override fun setDefaultSpeed(speed: Float) { prefs.edit().putFloat("default_speed", speed).apply() }
    override fun getDefaultCamera(): String? = prefs.getString("default_camera", null)
    override fun setDefaultCamera(preset: String?) {
        prefs.edit().apply {
            if (preset != null) putString("default_camera", preset) else remove("default_camera")
        }.apply()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences =
        SharedPreferencesUserPrefs(
            context.getSharedPreferences("morphocore_user_prefs", Context.MODE_PRIVATE)
        )
}
