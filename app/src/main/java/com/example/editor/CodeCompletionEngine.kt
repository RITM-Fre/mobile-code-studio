package com.example.editor

data class CompletionSuggestion(
    val label: String,
    val insertText: String,
    val replacePrefixLength: Int
)

object CodeCompletionEngine {

    private val htmlSnippets = listOf(
        "div" to "<div class=\"\"></div>",
        "section" to "<section>\n  \n</section>",
        "header" to "<header></header>",
        "main" to "<main></main>",
        "footer" to "<footer></footer>",
        "nav" to "<nav></nav>",
        "button" to "<button type=\"button\"></button>",
        "input" to "<input type=\"text\" placeholder=\"\" />",
        "link" to "<link rel=\"stylesheet\" href=\"style.css\" />",
        "script" to "<script src=\"script.js\"></script>",
        "style" to "<style>\n  \n</style>",
        "img" to "<img src=\"\" alt=\"\" />",
        "a" to "<a href=\"\"></a>",
        "h1" to "<h1></h1>",
        "p" to "<p></p>",
        "span" to "<span></span>",
        "ul" to "<ul>\n  <li></li>\n</ul>",
        "class" to "class=\"\"",
        "id" to "id=\"\""
    )

    private val cssSnippets = listOf(
        "display: flex;" to "display: flex;",
        "display: grid;" to "display: grid;",
        "justify-content" to "justify-content: center;",
        "align-items" to "align-items: center;",
        "background" to "background: ",
        "border-radius" to "border-radius: 16px;",
        "backdrop-filter" to "backdrop-filter: blur(12px);",
        "padding" to "padding: 16px;",
        "margin" to "margin: 0;",
        "color" to "color: ",
        "font-size" to "font-size: 16px;",
        "gap" to "gap: 12px;",
        "box-shadow" to "box-shadow: 0 12px 32px rgba(0,0,0,0.25);",
        "@media" to "@media (min-width: 640px) {\n  \n}"
    )

    private val jsSnippets = listOf(
        "const" to "const ",
        "let" to "let ",
        "function" to "function () {\n  \n}",
        "document.querySelector" to "document.querySelector(\"\")",
        "document.getElementById" to "document.getElementById(\"\")",
        "addEventListener" to "addEventListener(\"click\", () => {\n  \n});",
        "console.log" to "console.log();",
        "fetch" to "fetch(\"\").then(r => r.json())",
        "JSON.parse" to "JSON.parse()",
        "JSON.stringify" to "JSON.stringify()",
        "async" to "async function () {\n  \n}",
        "return" to "return "
    )

    private val jsonSnippets = listOf(
        "true" to "true",
        "false" to "false",
        "null" to "null"
    )

    fun suggest(
        text: String,
        cursorPosition: Int,
        language: CodeLanguage
    ): List<CompletionSuggestion> {
        val safeCursor = cursorPosition.coerceIn(0, text.length)
        var start = safeCursor
        while (start > 0) {
            val ch = text[start - 1]
            if (ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == '@' || ch == '.' || ch == ':') {
                start--
            } else {
                break
            }
        }
        val prefix = text.substring(start, safeCursor)
        if (prefix.isEmpty()) return emptyList()

        val candidates = when (language) {
            CodeLanguage.HTML, CodeLanguage.XML, CodeLanguage.SVG -> htmlSnippets
            CodeLanguage.CSS -> cssSnippets
            CodeLanguage.JAVASCRIPT -> jsSnippets
            CodeLanguage.JSON -> jsonSnippets
            else -> emptyList()
        }

        return candidates
            .filter { (label, _) ->
                label.startsWith(prefix, ignoreCase = true) && !label.equals(prefix, ignoreCase = false)
            }
            .take(6)
            .map { (label, insert) ->
                CompletionSuggestion(
                    label = label,
                    insertText = insert,
                    replacePrefixLength = prefix.length
                )
            }
    }
}
