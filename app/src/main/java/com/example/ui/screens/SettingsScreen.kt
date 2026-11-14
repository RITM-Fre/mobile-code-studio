package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AppConfig
import com.example.core.AppLanguage
import com.example.data.prefs.EditorFontFamily
import com.example.data.prefs.PreviewBehavior
import com.example.data.prefs.ThemeMode
import com.example.editor.CodeLanguage
import com.example.editor.SyntaxHighlighter
import com.example.ui.components.FloatingIslandHeader
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.formatByteSize
import com.example.ui.theme.LocalCodePalette
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.LocalStrings
import com.example.ui.theme.toComposeFontFamily
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val strings = LocalStrings.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val stats by viewModel.storageStats.collectAsStateWithLifecycle()
    val codePalette = LocalCodePalette.current
    val glassTokens = LocalGlassTokens.current

    val listState = rememberLazyListState()
    val isCompact by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 28 }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var portInput by remember(settings.serverPort) { mutableStateOf(settings.serverPort.toString()) }
    var portError by remember { mutableStateOf(false) }
    var symbolsInput by remember(settings.toolbarSymbols) { mutableStateOf(settings.toolbarSymbols) }

    LaunchedEffect(Unit) {
        viewModel.refreshStorageStats()
    }

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                FloatingIslandHeader(
                    title = strings.settings,
                    isCompact = isCompact,
                    leading = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 820.dp)
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Appearance & Language
                    item {
                        LiquidGlassCard {
                            SectionTitle(icon = Icons.Rounded.Language, title = strings.appearanceSection)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(strings.themeTitle, style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = settings.themeMode == ThemeMode.SYSTEM,
                                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                                    leadingIcon = { Icon(Icons.Rounded.SettingsBrightness, null, Modifier.size(16.dp)) },
                                    label = { Text(strings.themeSystem) },
                                    modifier = Modifier.testTag("theme_system_chip")
                                )
                                FilterChip(
                                    selected = settings.themeMode == ThemeMode.DARK,
                                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                                    leadingIcon = { Icon(Icons.Rounded.DarkMode, null, Modifier.size(16.dp)) },
                                    label = { Text(strings.themeDark) },
                                    modifier = Modifier.testTag("theme_dark_chip")
                                )
                                FilterChip(
                                    selected = settings.themeMode == ThemeMode.LIGHT,
                                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                                    leadingIcon = { Icon(Icons.Rounded.LightMode, null, Modifier.size(16.dp)) },
                                    label = { Text(strings.themeLight) },
                                    modifier = Modifier.testTag("theme_light_chip")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(strings.languageTitle, style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppLanguage.entries.forEach { lang ->
                                    FilterChip(
                                        selected = settings.language == lang,
                                        onClick = { viewModel.setLanguage(lang) },
                                        label = { Text(lang.displayName) },
                                        modifier = Modifier.testTag("lang_${lang.code}_chip")
                                    )
                                }
                            }
                        }
                    }

                    // Code Editor Settings
                    item {
                        LiquidGlassCard {
                            SectionTitle(icon = Icons.Rounded.Code, title = strings.editorSection)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.fontSize, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "${settings.editorFontSizeSp.roundToInt()} sp",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = settings.editorFontSizeSp,
                                onValueChange = { viewModel.setFontSize(it) },
                                valueRange = 11f..24f,
                                steps = 12
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(strings.fontFamily, style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                EditorFontFamily.entries.forEach { family ->
                                    FilterChip(
                                        selected = settings.editorFontFamily == family,
                                        onClick = { viewModel.setFontFamily(family) },
                                        label = { Text(family.label) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            val highlightedPreview = remember(settings.editorFontSizeSp, settings.editorFontFamily, glassTokens.isDark) {
                                SyntaxHighlighter.transform(
                                    AnnotatedString(strings.previewSampleCode),
                                    CodeLanguage.HTML,
                                    codePalette
                                ).text
                            }
                            androidx.compose.runtime.CompositionLocalProvider(
                                LocalLayoutDirection provides LayoutDirection.Ltr
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(glassTokens.editorBackground)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = highlightedPreview,
                                        style = TextStyle(
                                            fontFamily = settings.editorFontFamily.toComposeFontFamily(),
                                            fontSize = settings.editorFontSizeSp.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            ToggleRow(
                                title = strings.autoSave,
                                subtitle = strings.autoSaveDesc,
                                checked = settings.autoSave,
                                onCheckedChange = viewModel::setAutoSave
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            ToggleRow(
                                title = strings.lineNumbers,
                                subtitle = strings.lineNumbersDesc,
                                checked = settings.showLineNumbers,
                                onCheckedChange = viewModel::setLineNumbers
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            ToggleRow(
                                title = strings.wordWrap,
                                subtitle = strings.wordWrapDesc,
                                checked = settings.wordWrap,
                                onCheckedChange = viewModel::setWordWrap
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = symbolsInput,
                                onValueChange = {
                                    symbolsInput = it
                                    viewModel.setToolbarSymbols(it)
                                },
                                label = { Text(strings.keyboardToolbarTitle) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Server & Preview Settings
                    item {
                        LiquidGlassCard {
                            SectionTitle(icon = Icons.Rounded.Dns, title = strings.serverAndPreviewSection)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(strings.defaultPreviewBehavior, style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = settings.previewBehavior == PreviewBehavior.PROJECT_ENTRY,
                                    onClick = { viewModel.setPreviewBehavior(PreviewBehavior.PROJECT_ENTRY) },
                                    label = { Text(strings.previewProjectEntry) }
                                )
                                FilterChip(
                                    selected = settings.previewBehavior == PreviewBehavior.ACTIVE_HTML,
                                    onClick = { viewModel.setPreviewBehavior(PreviewBehavior.ACTIVE_HTML) },
                                    label = { Text(strings.previewActiveHtml) }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = portInput,
                                    onValueChange = { raw ->
                                        portInput = raw.filter { it.isDigit() }.take(5)
                                        val parsed = portInput.toIntOrNull()
                                        if (parsed != null && parsed in AppConfig.MIN_SERVER_PORT..AppConfig.MAX_SERVER_PORT) {
                                            portError = false
                                            viewModel.setServerPort(parsed)
                                        } else {
                                            portError = true
                                        }
                                    },
                                    label = { Text(strings.serverPort) },
                                    supportingText = {
                                        Text(if (portError) strings.invalidPortError else strings.serverPortHint)
                                    },
                                    isError = portError,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Storage & Maintenance
                    item {
                        LiquidGlassCard {
                            SectionTitle(icon = Icons.Rounded.Storage, title = strings.storageSection)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = strings.projectsAndFilesSummary(
                                    stats.projectCount,
                                    stats.totalFiles,
                                    formatByteSize(stats.totalBytes)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            viewModel.clearCache()
                                            snackbarHostState.showSnackbar(strings.cacheCleared)
                                        }
                                    },
                                    modifier = Modifier.testTag("clear_cache_button")
                                ) {
                                    Icon(Icons.Rounded.CleaningServices, null, Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.clearCache)
                                }
                                Button(
                                    onClick = onOpenAbout,
                                    modifier = Modifier.testTag("settings_about_button")
                                ) {
                                    Icon(Icons.Rounded.Info, null, Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.about)
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
