package app.adless.streaming.data

import android.content.Context
import app.adless.streaming.domain.ThemeMode

class LocalSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("adless_settings", Context.MODE_PRIVATE)

    fun enabledSources(defaults: Set<String>): Set<String> =
        preferences.getStringSet(KEY_ENABLED_SOURCES, null)?.toSet() ?: defaults

    fun setEnabledSources(ids: Set<String>) {
        preferences.edit().putStringSet(KEY_ENABLED_SOURCES, ids.toSet()).apply()
    }

    fun themeMode(): ThemeMode = runCatching {
        ThemeMode.valueOf(preferences.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name)
    }.getOrDefault(ThemeMode.LIGHT)

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    private companion object {
        const val KEY_ENABLED_SOURCES = "enabled_sources"
        const val KEY_THEME_MODE = "theme_mode"
    }
}

