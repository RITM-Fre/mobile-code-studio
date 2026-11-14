package com.example

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.core.ProjectTemplateId
import com.example.core.ProjectTemplates
import com.example.editor.CodeLanguage
import com.example.editor.EditorTextUtils
import com.example.editor.SyntaxHighlighter
import com.example.ui.theme.DarkSyntaxPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntaxHighlighterAndEditorUtilsTest {

    @Test
    fun syntaxHighlighterPreservesExactTextAndLength() {
        val samples = listOf(
            CodeLanguage.HTML to "<!DOCTYPE html><html><body class=\"main\"><!-- hi --><h1>Test</h1></body></html>",
            CodeLanguage.CSS to "body { margin: 0; color: #38bdf8; /* comment */ }",
            CodeLanguage.JAVASCRIPT to "const x = 42; function run() { return document.getElementById('a'); }",
            CodeLanguage.JSON to "{\"name\": \"Mobile Code Studio\", \"offline\": true}"
        )
        samples.forEach { (lang, code) ->
            val transformed = SyntaxHighlighter.transform(AnnotatedString(code), lang, DarkSyntaxPalette)
            assertEquals(code, transformed.text.text)
            assertTrue(transformed.text.spanStyles.isNotEmpty())
        }
    }

    @Test
    fun autoIndentIncreasesIndentAfterBrace() {
        val oldVal = TextFieldValue("function demo() {", TextRange(17))
        val newVal = TextFieldValue("function demo() {\n", TextRange(18))
        val indented = EditorTextUtils.applyAutoIndentIfNewline(oldVal, newVal)
        assertEquals("function demo() {\n  ", indented.text)
        assertEquals(20, indented.selection.start)
    }

    @Test
    fun bracketMatchFindsMatchingPair() {
        val code = "const obj = { a: [1, 2] };"
        val pair = EditorTextUtils.findMatchingBracket(code, 13)
        assertNotNull(pair)
        assertEquals(12, pair?.first)
        assertEquals(24, pair?.second)
    }

    @Test
    fun webBundleTemplateContainsLinkedHtmlCssJs() {
        val files = ProjectTemplates.generateStarterFiles(
            templateId = ProjectTemplateId.WEB_BUNDLE,
            projectName = "Demo",
            description = "Test"
        )
        assertTrue(files.containsKey("index.html"))
        assertTrue(files.containsKey("style.css"))
        assertTrue(files.containsKey("script.js"))
        assertTrue(files["index.html"]!!.contains("href=\"style.css\""))
        assertTrue(files["index.html"]!!.contains("src=\"script.js\""))
    }
}
