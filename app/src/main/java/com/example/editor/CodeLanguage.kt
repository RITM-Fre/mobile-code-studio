package com.example.editor

import java.util.Locale

enum class CodeLanguage(val label: String) {
    HTML("HTML"),
    CSS("CSS"),
    JAVASCRIPT("JS"),
    JSON("JSON"),
    XML("XML"),
    SVG("SVG"),
    MARKDOWN("MD"),
    PLAIN_TEXT("TXT");

    companion object {
        fun fromFileName(fileName: String): CodeLanguage {
            val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            return when (ext) {
                "html", "htm" -> HTML
                "css" -> CSS
                "js", "mjs" -> JAVASCRIPT
                "json" -> JSON
                "xml" -> XML
                "svg" -> SVG
                "md", "markdown" -> MARKDOWN
                else -> PLAIN_TEXT
            }
        }
    }
}
