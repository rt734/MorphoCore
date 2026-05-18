package com.morphocore.theme.impl.provider

import android.content.SharedPreferences

internal interface ThemePreferences {
    fun getLastThemeId(): String?
    fun saveThemeId(id: String)
}

internal class SharedPreferencesThemePrefs(
    private val prefs: SharedPreferences
) : ThemePreferences {
    override fun getLastThemeId(): String? = prefs.getString("active_theme_id", null)
    override fun saveThemeId(id: String) {
        prefs.edit().putString("active_theme_id", id).apply()
    }
}

internal class FakeThemePreferences : ThemePreferences {
    var lastId: String? = null
    override fun getLastThemeId(): String? = lastId
    override fun saveThemeId(id: String) { lastId = id }
}
