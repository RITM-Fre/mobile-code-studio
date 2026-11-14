package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.AppConfig
import com.example.core.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mobile_code_studio_prefs"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_SIZE = floatPreferencesKey("editor_font_size")
        val FONT_FAMILY = stringPreferencesKey("editor_font_family")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val LINE_NUMBERS = booleanPreferencesKey("line_numbers")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val PREVIEW_BEHAVIOR = stringPreferencesKey("preview_behavior")
        val SERVER_PORT = intPreferencesKey("server_port")
        val TOOLBAR_SYMBOLS = stringPreferencesKey("toolbar_symbols")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            editorFontSizeSp = (prefs[Keys.FONT_SIZE] ?: 14f).coerceIn(11f, 24f),
            editorFontFamily = EditorFontFamily.fromKey(prefs[Keys.FONT_FAMILY]),
            autoSave = prefs[Keys.AUTO_SAVE] ?: true,
            showLineNumbers = prefs[Keys.LINE_NUMBERS] ?: true,
            wordWrap = prefs[Keys.WORD_WRAP] ?: false,
            previewBehavior = PreviewBehavior.fromKey(prefs[Keys.PREVIEW_BEHAVIOR]),
            serverPort = (prefs[Keys.SERVER_PORT] ?: AppConfig.DEFAULT_SERVER_PORT)
                .coerceIn(AppConfig.MIN_SERVER_PORT, AppConfig.MAX_SERVER_PORT),
            toolbarSymbols = prefs[Keys.TOOLBAR_SYMBOLS] ?: AppConfig.DEFAULT_TOOLBAR_SYMBOLS
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = themeMode.key }
    }

    suspend fun setFontSize(sizeSp: Float) {
        context.settingsDataStore.edit { it[Keys.FONT_SIZE] = sizeSp.coerceIn(11f, 24f) }
    }

    suspend fun setFontFamily(fontFamily: EditorFontFamily) {
        context.settingsDataStore.edit { it[Keys.FONT_FAMILY] = fontFamily.key }
    }

    suspend fun setAutoSave(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_SAVE] = enabled }
    }

    suspend fun setLineNumbers(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.LINE_NUMBERS] = enabled }
    }

    suspend fun setWordWrap(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.WORD_WRAP] = enabled }
    }

    suspend fun setPreviewBehavior(behavior: PreviewBehavior) {
        context.settingsDataStore.edit { it[Keys.PREVIEW_BEHAVIOR] = behavior.key }
    }

    suspend fun setServerPort(port: Int) {
        val clamped = port.coerceIn(AppConfig.MIN_SERVER_PORT, AppConfig.MAX_SERVER_PORT)
        context.settingsDataStore.edit { it[Keys.SERVER_PORT] = clamped }
    }

    suspend fun setToolbarSymbols(symbols: String) {
        context.settingsDataStore.edit {
            it[Keys.TOOLBAR_SYMBOLS] = symbols.ifBlank { AppConfig.DEFAULT_TOOLBAR_SYMBOLS }
        }
    }
}
