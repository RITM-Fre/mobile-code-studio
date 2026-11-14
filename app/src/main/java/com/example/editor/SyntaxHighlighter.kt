package com.example.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

data class CodeSyntaxPalette(
    val plain: Color,
    val keyword: Color,
    val tag: Color,
    val attribute: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val functionName: Color,
    val punctuation: Color,
    val findHighlightBg: Color,
    val activeFindHighlightBg: Color,
    val bracketMatchBg: Color
)

object SyntaxHighlighter {

    private val JS_KEYWORDS = setOf(
        "const", "let", "var", "function", "return", "if", "else", "for", "while", "do",
        "switch", "case", "break", "continue", "default", "try", "catch", "finally",
        "throw", "new", "class", "extends", "import", "export", "from", "async", "await",
        "true", "false", "null", "undefined", "this", "typeof", "instanceof", "of", "in"
    )

    private val JS_BUILTINS = setOf(
        "document", "window", "console", "Math", "JSON", "Array", "Object", "String",
        "Number", "Boolean", "Promise", "Date", "fetch", "setTimeout", "setInterval",
        "localStorage", "sessionStorage", "navigator"
    )

    private val CSS_AT_RULES = setOf(
        "@media", "@keyframes", "@import", "@font-face", "@supports", "@layer", ":root"
    )

    fun transform(
        text: AnnotatedString,
        language: CodeLanguage,
        palette: CodeSyntaxPalette,
        findRanges: List<IntRange> = emptyList(),
        activeFindIndex: Int = -1,
        bracketMatchPair: Pair<Int, Int>? = null
    ): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Bound expensive tokenizing on ultra-massive files (>180k chars) for guaranteed 60fps UI
        val highlightLimit = minOf(raw.length, 180_000)
        val builder = AnnotatedString.Builder(raw)
        builder.addStyle(SpanStyle(color = palette.plain), 0, raw.length)

        when (language) {
            CodeLanguage.HTML, CodeLanguage.XML, CodeLanguage.SVG ->
                highlightMarkup(raw, highlightLimit, builder, palette)
            CodeLanguage.CSS ->
                highlightCss(raw, highlightLimit, builder, palette)
            CodeLanguage.JAVASCRIPT ->
                highlightJavaScript(raw, highlightLimit, builder, palette)
            CodeLanguage.JSON ->
                highlightJson(raw, highlightLimit, builder, palette)
            CodeLanguage.MARKDOWN ->
                highlightMarkdown(raw, highlightLimit, builder, palette)
            CodeLanguage.PLAIN_TEXT -> Unit
        }

        // Apply find highlights
        findRanges.forEachIndexed { idx, range ->
            val start = range.first.coerceIn(0, raw.length)
            val end = (range.last + 1).coerceIn(start, raw.length)
            if (start < end) {
                val bg = if (idx == activeFindIndex) palette.activeFindHighlightBg else palette.findHighlightBg
                builder.addStyle(SpanStyle(background = bg), start, end)
            }
        }

        // Apply matched bracket highlight
        bracketMatchPair?.let { (firstIdx, secondIdx) ->
            if (firstIdx in raw.indices) {
                builder.addStyle(
                    SpanStyle(background = palette.bracketMatchBg, fontWeight = FontWeight.Bold),
                    firstIdx,
                    firstIdx + 1
                )
            }
            if (secondIdx in raw.indices) {
                builder.addStyle(
                    SpanStyle(background = palette.bracketMatchBg, fontWeight = FontWeight.Bold),
                    secondIdx,
                    secondIdx + 1
                )
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    fun visualTransformation(
        language: CodeLanguage,
        palette: CodeSyntaxPalette,
        findRanges: List<IntRange>,
        activeFindIndex: Int,
        bracketMatchPair: Pair<Int, Int>?
    ): VisualTransformation = VisualTransformation { annotated ->
        transform(
            text = annotated,
            language = language,
            palette = palette,
            findRanges = findRanges,
            activeFindIndex = activeFindIndex,
            bracketMatchPair = bracketMatchPair
        )
    }

    private fun highlightMarkup(
        text: String,
        limit: Int,
        builder: AnnotatedString.Builder,
        palette: CodeSyntaxPalette
    ) {
        var i = 0
        while (i < limit) {
            if (text.startsWith("<!--", i)) {
                val end = text.indexOf("-->", i + 4).let { if (it == -1) limit else minOf(it + 3, limit) }
                builder.addStyle(SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic), i, end)
                i = end
                continue
            }
            if (text[i] == '<') {
                val tagClose = findMarkupTagEnd(text, i, limit)
                if (tagClose > i) {
                    highlightMarkupTag(text, i, tagClose, builder, palette)
                    i = tagClose
                    continue
                }
            }
            i++
        }
    }

    private fun findMarkupTagEnd(text: String, start: Int, limit: Int): Int {
        var inSingle = false
        var inDouble = false
        var i = start + 1
        while (i < limit) {
            val c = text[i]
            if (c == '"' && !inSingle) inDouble = !inDouble
            else if (c == '\'' && !inDouble) inSingle = !inSingle
            else if (c == '>' && !inSingle && !inDouble) return i + 1
            i++
        }
        return limit
    }

    private fun highlightMarkupTag(
        text: String,
        start: Int,
        end: Int,
        builder: AnnotatedString.Builder,
        palette: CodeSyntaxPalette
    ) {
        var i = start
        // '<' or '</' or '<!'
        val bracketStart = i
        i++
        if (i < end && (text[i] == '/' || text[i] == '!' || text[i] == '?')) i++
        builder.addStyle(SpanStyle(color = palette.punctuation), bracketStart, i)

        // Tag name
        val nameStart = i
        while (i < end && (text[i].isLetterOrDigit() || text[i] == '-' || text[i] == ':' || text[i] == '_')) {
            i++
        }
        if (i > nameStart) {
            builder.addStyle(SpanStyle(color = palette.tag, fontWeight = FontWeight.SemiBold), nameStart, i)
        }

        // Attributes and strings inside tag
        while (i < end) {
            val c = text[i]
            when {
                c == '"' || c == '\'' -> {
                    val qStart = i
                    i++
                    while (i < end && text[i] != c) i++
                    if (i < end) i++
                    builder.addStyle(SpanStyle(color = palette.string), qStart, i)
                }
                c == '/' || c == '>' || c == '=' || c == '?' -> {
                    builder.addStyle(SpanStyle(color = palette.punctuation), i, i + 1)
                    i++
                }
                c.isLetter() || c == '-' || c == ':' || c == '@' -> {
                    val attrStart = i
                    while (i < end && (text[i].isLetterOrDigit() || text[i] in charArrayOf('-', '_', ':', '.', '@'))) {
                        i++
                    }
                    builder.addStyle(SpanStyle(color = palette.attribute), attrStart, i)
                }
                else -> i++
            }
        }
    }

    private fun highlightCss(
        text: String,
        limit: Int,
        builder: AnnotatedString.Builder,
        palette: CodeSyntaxPalette
    ) {
        var i = 0
        var insideBlock = false
        while (i < limit) {
            val c = text[i]
            when {
                c == '/' && i + 1 < limit && text[i + 1] == '*' -> {
                    val end = text.indexOf("*/", i + 2).let { if (it == -1) limit else minOf(it + 2, limit) }
                    builder.addStyle(SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic), i, end)
                    i = end
                }
                c == '"' || c == '\'' -> {
                    val qStart = i++
                    while (i < limit && text[i] != c) {
                        if (text[i] == '\\' && i + 1 < limit) i++
                        i++
                    }
                    if (i < limit) i++
                    builder.addStyle(SpanStyle(color = palette.string), qStart, i)
                }
                c == '{' -> {
                    insideBlock = true
                    builder.addStyle(SpanStyle(color = palette.punctuation), i, i + 1)
                    i++
                }
                c == '}' -> {
                    insideBlock = false
                    builder.addStyle(SpanStyle(color = palette.punctuation), i, i + 1)
                    i++
                }
                c == '#' -> {
                    val startHex = i++
                    while (i < limit && (text[i].isLetterOrDigit() || text[i] == '-' || text[i] == '_')) i++
                    val color = if (insideBlock) palette.number else palette.tag
                    builder.addStyle(SpanStyle(color = color), startHex, i)
                }
                c == '@' -> {
                    val atStart = i++
                    while (i < limit && (text[i].isLetterOrDigit() || text[i] == '-')) i++
                    val word = text.substring(atStart, i)
                    val color = if (word in CSS_AT_RULES) palette.keyword else palette.attribute
                    builder.addStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold), atStart, i)
                }
                c.isDigit() -> {
                    val numStart = i++
                    while (i < limit && (text[i].isLetterOrDigit() || text[i] == '.' || text[i] == '%')) i++
                    builder.addStyle(SpanStyle(color = palette.number), numStart, i)
                }
                c.isLetter() || c == '-' || c == '.' -> {
                    val tokStart = i++
                    while (i < limit && (text[i].isLetterOrDigit() || text[i] == '-' || text[i] == '_')) i++
                    // Check if followed by ':' inside block -> CSS property
                    var look = i
                    while (look < limit && text[look] == ' ') look++
                    if (insideBlock && look < limit && text[look] == ':') {
                        builder.addStyle(SpanStyle(color = palette.attribute), tokStart, i)
                    } else if (!insideBlock) {
                        builder.addStyle(SpanStyle(color = palette.tag), tokStart, i)
                    } else {
                        builder.addStyle(SpanStyle(color = palette.functionName), tokStart, i)
                    }
                }
                c in charArrayOf(':', ';', '(', ')', ',') -> {
                    builder.addStyle(SpanStyle(color = palette.punctuation), i, i + 1)
                    i++
                }
                else -> i++
            }
        }
    }

    private fun highlightJavaScript(
        text: String,
        limit: Int,
        builder: AnnotatedString.Builder,
        palette: CodeSyntaxPalette
    ) {
        var i = 0
        while (i < limit) {
            val c = text[i]
            when {
                c == '/' && i + 1 < limit && text[i + 1] == '/' -> {
                    val end = text.indexOf('\n', i + 2).let { if (it == -1) limit else minOf(it, limit) }
                    builder.addStyle(SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic), i, end)
                    i = end
                }
                c == '/' && i + 1 < limit && text[i + 1] == '*' -> {
                    val end = text.indexOf("*/", i + 2).let { if (it == -1) limit else minOf(it + 2, limit) }
                    builder.addStyle(SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic), i, end)
                    i = end
                }
                c == '"' || c == '\'' || c == '`' -> {
                    val qStart = i++
                    while (i < limit && text[i] != c) {
                        if (text[i] == '\\' && i + 1 < limit) i++
                        i++
                    }
                    if (i < limit) i++
                    builder.addStyle(SpanStyle(color = palette.string), qStart, i)
                }
                c.isDigit() -> {
                    val numStart = i++
                    while (i < limit && (text[i].isLetterOrDigit() || text[i] == '.' || text[i] == '_')) i++
                    builder.addStyle(SpanStyle(color = palette.number), numStart, i)
                }
                c.isLetter() || c == '_' || c == '$' -> {
                    val idStart = i++
                    while (i < limit && (text[i].isLetterOrDigit() || text[i] == '_' || text[i] == '$')) i++
                    val word = text.substring(idStart, i)
                    when {
                        word in JS_KEYWORDS ->
                            builder.addStyle(SpanStyle(color = palette.keyword, fontWeight = FontWeight.SemiBold), idStart, i)
                        word in JS_BUILTINS ->
                            builder.addStyle(SpanStyle(color = palette.tag), idStart, i)
                        i < limit && text[i] == '(' ->
                            builder.addStyle(SpanStyle(color = palette.functionName), idStart, i)
                    }
                }
                c in charArrayOf('{', '}', '(', ')', '[', ']', ';', ',', '=', '+', '-', '*', '<', '>', '!', '&', '|') -> {
                    builder.addStyle(SpanStyle(color = palette.punctuation), i, i + 1)
                    i++
                }
                else -> i++
            }
        }
    }

    private fun highlightJson(
        text: String,
        limit: Int,
        builder: AnnotatedString.Builder,
        palette: CodeSyntaxPalette
    ) {
        var i = 0
        while (i < limit) {
            val c = text[i]
            when {
                c == '"' -> {
                    val qStart = i++
                    while (i < limit && text[i] != '"') {
                        if (text[i] == '\\' && i + 1 < limit) i++
                        i++
                    }
                    if (i < limit) i++
                    var look = i
                    while (look < limit && text[look].isWhitespace()) look++
                    val isKey = look < limit && text[look] == ':'
                    val color = if (isKey) palette.attribute else palette.string
                    builder.addStyle(SpanStyle(color = color), qStart, i)
                }
                c.isDigit() || (c == '-' && i + 1 < limit && text[i + 1].isDigit()) -> {
                    val numStart = i++
                    while (i < limit && (text[i].isDigit() || text[i] in charArrayOf('.', 'e', 'E', '+', '-'))) i++
                    builder.addStyle(SpanStyle(color = palette.number), numStart, i)
                }
                c.isLetter() -> {
                    val wStart = i++
                    while (i < limit && text[i].isLetter()) i++
                    val word = text.substring(wStart, i)
                    if (word == "true" || word == "false" || word == "null") {
                        builder.addStyle(SpanStyle(color = palette.keyword, fontWeight = FontWeight.SemiBold), wStart, i)
                    }
                }
                c in charArrayOf('{', '}', '[', ']', ':', ',') -> {
                    builder.addStyle(SpanStyle(color = palette.punctuation), i, i + 1)
                    i++
                }
                else -> i++
            }
        }
    }

    private fun highlightMarkdown(
        text: String,
        limit: Int,
        builder: AnnotatedString.Builder,
        palette: CodeSyntaxPalette
    ) {
        var i = 0
        while (i < limit) {
            val lineEnd = text.indexOf('\n', i).let { if (it == -1) limit else minOf(it, limit) }
            if (i < lineEnd) {
                val trimmed = text.substring(i, lineEnd)
                when {
                    trimmed.startsWith("#") ->
                        builder.addStyle(SpanStyle(color = palette.tag, fontWeight = FontWeight.Bold), i, lineEnd)
                    trimmed.startsWith("```") ->
                        builder.addStyle(SpanStyle(color = palette.keyword), i, lineEnd)
                    trimmed.startsWith(">") ->
                        builder.addStyle(SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic), i, lineEnd)
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") ->
                        builder.addStyle(SpanStyle(color = palette.punctuation), i, i + 1)
                }
            }
            i = lineEnd + 1
        }
    }
}
