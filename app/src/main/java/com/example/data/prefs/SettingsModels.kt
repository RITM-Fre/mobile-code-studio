package com.example.data.prefs

import com.example.core.AppConfig
import com.example.core.AppLanguage

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: DARK
    }
}

enum class EditorFontFamily(val key: String, val label: String) {
    JETBRAINS_MONO("jetbrains_mono", "JetBrains Mono"),
    FIRA_CODE("fira_code", "Fira Code"),
    ROBOTO_MONO("roboto_mono", "Roboto Mono"),
    SYSTEM_MONO("system_mono", "System Monospace");

    companion object {
        fun fromKey(key: String?): EditorFontFamily =
            entries.firstOrNull { it.key == key } ?: JETBRAINS_MONO
    }
}

enum class PreviewBehavior(val key: String) {
    PROJECT_ENTRY("project_entry"),
    ACTIVE_HTML("active_html");

    companion object {
        fun fromKey(key: String?): PreviewBehavior =
            entries.firstOrNull { it.key == key } ?: PROJECT_ENTRY
    }
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val editorFontSizeSp: Float = 14f,
    val editorFontFamily: EditorFontFamily = EditorFontFamily.JETBRAINS_MONO,
    val autoSave: Boolean = true,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val previewBehavior: PreviewBehavior = PreviewBehavior.PROJECT_ENTRY,
    val serverPort: Int = AppConfig.DEFAULT_SERVER_PORT,
    val toolbarSymbols: String = AppConfig.DEFAULT_TOOLBAR_SYMBOLS
)
