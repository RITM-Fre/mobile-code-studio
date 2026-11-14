package com.example.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object EditorTextUtils {

    private val VOID_HTML_TAGS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr"
    )

    /**
     * Applies smart auto-indentation when user inserts a single newline.
     */
    fun applyAutoIndentIfNewline(
        oldValue: TextFieldValue,
        newValue: TextFieldValue
    ): TextFieldValue {
        val oldText = oldValue.text
        val newText = newValue.text
        if (newText.length != oldText.length + 1) return newValue
        val cursor = newValue.selection.start
        if (cursor <= 0 || cursor > newText.length) return newValue
        if (newText[cursor - 1] != '\n') return newValue

        val lineStart = newText.lastIndexOf('\n', (cursor - 2).coerceAtLeast(0)).let {
            if (it == -1) 0 else it + 1
        }
        val prevLine = newText.substring(lineStart, cursor - 1)
        val baseIndent = prevLine.takeWhile { it == ' ' || it == '\t' }
        val trimmed = prevLine.trimEnd()

        val shouldIncrease = trimmed.endsWith("{") ||
            trimmed.endsWith("(") ||
            trimmed.endsWith("[") ||
            isUnclosedOpeningHtmlTag(trimmed)

        val extra = if (shouldIncrease) "  " else ""
        val indent = baseIndent + extra
        if (indent.isEmpty()) return newValue

        val updated = StringBuilder(newText).insert(cursor, indent).toString()
        val newCursor = cursor + indent.length
        return newValue.copy(
            text = updated,
            selection = TextRange(newCursor)
        )
    }

    private fun isUnclosedOpeningHtmlTag(trimmedLine: String): Boolean {
        if (!trimmedLine.startsWith("<") || !trimmedLine.endsWith(">")) return false
        if (trimmedLine.startsWith("</") || trimmedLine.startsWith("<!") || trimmedLine.endsWith("/>")) return false
        val tagName = trimmedLine.drop(1).takeWhile { it.isLetterOrDigit() }.lowercase()
        if (tagName.isEmpty() || tagName in VOID_HTML_TAGS) return false
        return !trimmedLine.contains("</$tagName>")
    }

    fun insertSymbolAtCursor(value: TextFieldValue, symbol: String): TextFieldValue {
        val text = value.text
        val start = minOf(value.selection.start, value.selection.end).coerceIn(0, text.length)
        val end = maxOf(value.selection.start, value.selection.end).coerceIn(0, text.length)
        val updated = text.substring(0, start) + symbol + text.substring(end)
        val newCursor = when (symbol) {
            "{}", "[]", "()", "\"\"", "''", "<>" -> start + 1
            else -> start + symbol.length
        }
        return TextFieldValue(
            text = updated,
            selection = TextRange(newCursor)
        )
    }

    fun applyCompletion(value: TextFieldValue, suggestion: CompletionSuggestion): TextFieldValue {
        val cursor = value.selection.start.coerceIn(0, value.text.length)
        val replaceStart = (cursor - suggestion.replacePrefixLength).coerceAtLeast(0)
        val updated = value.text.substring(0, replaceStart) +
            suggestion.insertText +
            value.text.substring(cursor)
        val caretInside = suggestion.insertText.indexOf("\"\"").let {
            if (it >= 0) replaceStart + it + 1
            else suggestion.insertText.indexOf("></").let { tagGap ->
                if (tagGap >= 0) replaceStart + tagGap + 1
                else replaceStart + suggestion.insertText.length
            }
        }
        return TextFieldValue(
            text = updated,
            selection = TextRange(caretInside)
        )
    }

    fun findAllMatches(text: String, query: String, matchCase: Boolean): List<IntRange> {
        if (query.isEmpty() || text.isEmpty()) return emptyList()
        val results = mutableListOf<IntRange>()
        var index = text.indexOf(query, startIndex = 0, ignoreCase = !matchCase)
        while (index >= 0) {
            results.add(index until (index + query.length))
            index = text.indexOf(query, startIndex = index + maxOf(1, query.length), ignoreCase = !matchCase)
            if (results.size >= 2000) break
        }
        return results
    }

    fun findMatchingBracket(text: String, cursorPosition: Int): Pair<Int, Int>? {
        if (text.isEmpty()) return null
        val candidates = listOf(cursorPosition - 1, cursorPosition)
        for (idx in candidates) {
            if (idx !in text.indices) continue
            val ch = text[idx]
            when (ch) {
                '(', '{', '[', '<' -> {
                    val close = matchingClose(ch)
                    val match = scanForward(text, idx, ch, close)
                    if (match != null) return idx to match
                }
                ')', '}', ']', '>' -> {
                    val open = matchingOpen(ch)
                    val match = scanBackward(text, idx, ch, open)
                    if (match != null) return match to idx
                }
            }
        }
        return null
    }

    private fun matchingClose(open: Char): Char = when (open) {
        '(' -> ')'
        '{' -> '}'
        '[' -> ']'
        '<' -> '>'
        else -> open
    }

    private fun matchingOpen(close: Char): Char = when (close) {
        ')' -> '('
        '}' -> '{'
        ']' -> '['
        '>' -> '<'
        else -> close
    }

    private fun scanForward(text: String, start: Int, open: Char, close: Char): Int? {
        var depth = 0
        val limit = minOf(text.length, start + 12_000)
        for (i in start until limit) {
            when (text[i]) {
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }

    private fun scanBackward(text: String, start: Int, close: Char, open: Char): Int? {
        var depth = 0
        val floor = maxOf(0, start - 12_000)
        for (i in start downTo floor) {
            when (text[i]) {
                close -> depth++
                open -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }

    fun formatBasicIndentation(text: String): String {
        val lines = text.split('\n')
        val out = StringBuilder()
        var indent = 0
        lines.forEachIndexed { idx, rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.startsWith("}") || trimmed.startsWith("]") || trimmed.startsWith(")") || trimmed.startsWith("</")) {
                indent = maxOf(0, indent - 1)
            }
            if (trimmed.isNotEmpty()) {
                out.append("  ".repeat(indent)).append(trimmed)
            }
            if (idx < lines.lastIndex) out.append('\n')
            if (trimmed.endsWith("{") || trimmed.endsWith("[") || trimmed.endsWith("(") || isUnclosedOpeningHtmlTag(trimmed)) {
                indent++
            }
        }
        return out.toString()
    }
}
