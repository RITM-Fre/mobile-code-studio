package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.prefs.AppSettings
import com.example.editor.CompletionSuggestion
import com.example.editor.SyntaxHighlighter
import com.example.ui.theme.LocalCodePalette
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.LocalStrings
import com.example.ui.theme.toComposeFontFamily
import com.example.ui.viewmodel.EditorTabState

@Composable
fun CodeEditorView(
    tab: EditorTabState,
    settings: AppSettings,
    completions: List<CompletionSuggestion>,
    isFindVisible: Boolean,
    findQuery: String,
    replaceQuery: String,
    matchCase: Boolean,
    findMatches: List<IntRange>,
    activeFindIndex: Int,
    bracketMatchPair: Pair<Int, Int>?,
    onValueChange: (TextFieldValue) -> Unit,
    onInsertSymbol: (String) -> Unit,
    onApplyCompletion: (CompletionSuggestion) -> Unit,
    onFindQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onToggleMatchCase: () -> Unit,
    onFindPrev: () -> Unit,
    onFindNext: () -> Unit,
    onReplaceOne: () -> Unit,
    onReplaceAll: () -> Unit,
    onCloseFind: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val tokens = LocalGlassTokens.current
    val codePalette = LocalCodePalette.current

    val codeFont = settings.editorFontFamily.toComposeFontFamily()
    val fontSize = settings.editorFontSizeSp.sp
    val lineHeight = (settings.editorFontSizeSp * 1.48f).sp

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val lineCount = remember(tab.textFieldValue.text) {
        tab.textFieldValue.text.count { it == '\n' } + 1
    }

    val visualTransformation = remember(
        tab.language,
        codePalette,
        findMatches,
        activeFindIndex,
        bracketMatchPair
    ) {
        SyntaxHighlighter.visualTransformation(
            language = tab.language,
            palette = codePalette,
            findRanges = findMatches,
            activeFindIndex = activeFindIndex,
            bracketMatchPair = bracketMatchPair
        )
    }

    val symbolTokens = remember(settings.toolbarSymbols) {
        val parsed = settings.toolbarSymbols.split(' ').filter { it.isNotBlank() }
        listOf("TAB") + parsed
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.editorBackground)
    ) {
        // Inline Find & Replace Panel
        AnimatedVisibility(visible = isFindVisible) {
            Surface(
                color = tokens.glassCardElevated,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .border(1.dp, tokens.glassBorderStart, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = findQuery,
                            onValueChange = onFindQueryChange,
                            placeholder = { Text(strings.findHint, style = MaterialTheme.typography.labelMedium) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("editor_find_input")
                        )
                        FilterChip(
                            selected = matchCase,
                            onClick = onToggleMatchCase,
                            label = { Text(strings.matchCase) }
                        )
                        IconButton(onClick = onFindPrev, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
                        }
                        IconButton(onClick = onFindNext, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                        }
                        IconButton(onClick = onCloseFind, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = null)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = replaceQuery,
                            onValueChange = onReplaceQueryChange,
                            placeholder = { Text(strings.replaceHint, style = MaterialTheme.typography.labelMedium) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("editor_replace_input")
                        )
                        Text(
                            text = if (findMatches.isEmpty()) "0" else "${activeFindIndex + 1}/${findMatches.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onReplaceOne, enabled = findMatches.isNotEmpty()) {
                            Text(strings.replaceOne)
                        }
                        TextButton(onClick = onReplaceAll, enabled = findMatches.isNotEmpty()) {
                            Text(strings.replaceAll)
                        }
                    }
                }
            }
        }

        // Main Code Editor Area (Always LTR for code readability)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(verticalScrollState)
            ) {
                if (settings.showLineNumbers) {
                    val gutterText = remember(lineCount) {
                        (1..lineCount).joinToString("\n")
                    }
                    val gutterDigits = lineCount.toString().length.coerceAtLeast(2)
                    Box(
                        modifier = Modifier
                            .width((gutterDigits * 10 + 18).dp)
                            .fillMaxHeight()
                            .background(tokens.editorGutter)
                            .padding(vertical = 14.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = gutterText,
                            style = TextStyle(
                                fontFamily = codeFont,
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.End
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (settings.wordWrap) Modifier.fillMaxWidth()
                            else Modifier.horizontalScroll(horizontalScrollState)
                        )
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    BasicTextField(
                        value = tab.textFieldValue,
                        onValueChange = onValueChange,
                        textStyle = TextStyle(
                            fontFamily = codeFont,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            color = codePalette.plain
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = visualTransformation,
                        onTextLayout = { textLayoutResult = it },
                        modifier = Modifier
                            .then(
                                if (settings.wordWrap) Modifier.fillMaxWidth()
                                else Modifier.widthIn(min = 900.dp)
                            )
                            .testTag("code_editor_textfield")
                    )
                }
            }
        }

        // Smart Code Completion + Configurable Keyboard Programming Toolbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.glassCardElevated)
                .navigationBarsPadding()
                .imePadding()
        ) {
            AnimatedVisibility(visible = completions.isNotEmpty()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        items(completions, key = { it.label }) { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onApplyCompletion(suggestion) }
                            ) {
                                Text(
                                    text = suggestion.label,
                                    style = TextStyle(
                                        fontFamily = codeFont,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(symbolTokens) { token ->
                        val insertValue = if (token == "TAB") "  " else token
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .height(36.dp)
                                .widthIn(min = 38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onInsertSymbol(insertValue) }
                                .testTag("symbol_key_$token")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                                Text(
                                    text = token,
                                    style = TextStyle(
                                        fontFamily = codeFont,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
