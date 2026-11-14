package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.fs.FileNode
import com.example.data.prefs.AppSettings
import com.example.server.ServerStatus
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.MoveItemDialog
import com.example.ui.components.TextInputDialog
import com.example.ui.components.fileVisualFor
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.LocalStrings
import com.example.ui.viewmodel.WorkspaceEvent
import com.example.ui.viewmodel.WorkspaceViewModel
import kotlinx.coroutines.launch

@Composable
fun WorkspaceScreen(
    projectId: String,
    initialFile: String?,
    viewModel: WorkspaceViewModel,
    settings: AppSettings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunchPreview: (previewUrl: String, entryPath: String) -> Unit
) {
    val strings = LocalStrings.current
    val tokens = LocalGlassTokens.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var createFileParent by remember { mutableStateOf<String?>(null) }
    var createFolderParent by remember { mutableStateOf<String?>(null) }
    var itemToRename by remember { mutableStateOf<FileNode?>(null) }
    var itemToMove by remember { mutableStateOf<FileNode?>(null) }
    var itemToDelete by remember { mutableStateOf<FileNode?>(null) }
    var moreMenuOpen by remember { mutableStateOf(false) }
    var runMenuOpen by remember { mutableStateOf(false) }

    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportProjectZip(uri, strings.projectExported, strings.operationFailed)
        }
    }

    LaunchedEffect(projectId, initialFile) {
        viewModel.loadProjectIfNeeded(projectId, initialFile)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WorkspaceEvent.LaunchPreview -> onLaunchPreview(event.previewUrl, event.entryPath)
                is WorkspaceEvent.ToastMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletSplit = maxWidth >= 840.dp

        val explorerContent: @Composable (Modifier) -> Unit = { mod ->
            FileExplorerPanel(
                projectName = state.project?.name ?: strings.explorer,
                tree = state.fileTree,
                expandedFolders = state.expandedFolders,
                activeFilePath = state.activeTabPath,
                searchQuery = state.fileSearchQuery,
                onSearchChange = viewModel::updateFileSearch,
                onToggleFolder = viewModel::toggleFolderExpanded,
                onOpenFile = { path ->
                    viewModel.openFile(path)
                    if (!isTabletSplit) scope.launch { drawerState.close() }
                },
                onPreviewHtmlFile = { path ->
                    if (!isTabletSplit) scope.launch { drawerState.close() }
                    viewModel.openFile(path)
                    viewModel.launchPreview(forceProjectEntry = false, noHtmlError = strings.noHtmlEntryFound)
                },
                onCreateFileRequest = { parent -> createFileParent = parent },
                onCreateFolderRequest = { parent -> createFolderParent = parent },
                onRenameRequest = { node -> itemToRename = node },
                onDuplicateRequest = { node ->
                    viewModel.duplicateItem(node.relativePath, strings.operationFailed)
                },
                onMoveRequest = { node -> itemToMove = node },
                onDeleteRequest = { node -> itemToDelete = node },
                modifier = mod
            )
        }

        val workspaceBody: @Composable () -> Unit = {
            LiquidGlassBackground {
                Scaffold(
                    containerColor = Color.Transparent,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = tokens.glassCardElevated,
                            border = BorderStroke(1.dp, tokens.glassBorderStart)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onBack, modifier = Modifier.testTag("workspace_back_button")) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                                }

                                if (!isTabletSplit) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("workspace_drawer_button")
                                    ) {
                                        Icon(Icons.Rounded.FolderOpen, contentDescription = strings.toggleExplorer)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.project?.name ?: strings.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val statusText = when (val s = serverStatus) {
                                        is ServerStatus.Running -> strings.serverRunningAt(s.port)
                                        is ServerStatus.Error -> strings.serverError
                                        ServerStatus.Stopped -> strings.serverStopped
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (serverStatus is ServerStatus.Running) EmeraldMint
                                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                val activeTab = state.activeTab
                                IconButton(
                                    onClick = viewModel::undo,
                                    enabled = activeTab?.undoStack?.isNotEmpty() == true,
                                    modifier = Modifier.testTag("editor_undo_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = strings.undo)
                                }
                                IconButton(
                                    onClick = viewModel::redo,
                                    enabled = activeTab?.redoStack?.isNotEmpty() == true,
                                    modifier = Modifier.testTag("editor_redo_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = strings.redo)
                                }
                                IconButton(
                                    onClick = { viewModel.saveActiveFile(strings.saved) },
                                    enabled = activeTab != null,
                                    modifier = Modifier.testTag("editor_save_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Save,
                                        contentDescription = strings.save,
                                        tint = if (activeTab?.isDirty == true) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }

                                // Run button + mode selector for HTML file vs Project
                                Box {
                                    Button(
                                        onClick = {
                                            val activeIsHtml = activeTab?.relativePath?.let {
                                                it.endsWith(".html", true) || it.endsWith(".htm", true)
                                            } == true
                                            if (activeIsHtml) {
                                                runMenuOpen = true
                                            } else {
                                                viewModel.launchPreview(
                                                    forceProjectEntry = true,
                                                    noHtmlError = strings.noHtmlEntryFound
                                                )
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .testTag("workspace_run_button")
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.run, style = MaterialTheme.typography.labelLarge)
                                    }

                                    DropdownMenu(
                                        expanded = runMenuOpen,
                                        onDismissRequest = { runMenuOpen = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(strings.runProject) },
                                            leadingIcon = { Icon(Icons.Rounded.PlayArrow, null) },
                                            onClick = {
                                                runMenuOpen = false
                                                viewModel.launchPreview(
                                                    forceProjectEntry = true,
                                                    noHtmlError = strings.noHtmlEntryFound
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("${strings.runThisFile} (${activeTab?.fileName ?: ""})") },
                                            leadingIcon = { Icon(Icons.Rounded.Code, null) },
                                            onClick = {
                                                runMenuOpen = false
                                                viewModel.launchPreview(
                                                    forceProjectEntry = false,
                                                    noHtmlError = strings.noHtmlEntryFound
                                                )
                                            }
                                        )
                                    }
                                }

                                Box {
                                    IconButton(
                                        onClick = { moreMenuOpen = true },
                                        modifier = Modifier.testTag("workspace_more_button")
                                    ) {
                                        Icon(Icons.Rounded.MoreVert, contentDescription = strings.moreOptions)
                                    }
                                    DropdownMenu(
                                        expanded = moreMenuOpen,
                                        onDismissRequest = { moreMenuOpen = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(strings.findAndReplace) },
                                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
                                            onClick = {
                                                moreMenuOpen = false
                                                viewModel.toggleFindBar()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.autoIndentLine) },
                                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.FormatIndentIncrease, null) },
                                            enabled = activeTab != null,
                                            onClick = {
                                                moreMenuOpen = false
                                                viewModel.formatCurrentFileIndentation()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.selectAll) },
                                            leadingIcon = { Icon(Icons.Rounded.SelectAll, null) },
                                            enabled = activeTab != null,
                                            onClick = {
                                                moreMenuOpen = false
                                                viewModel.selectAllText()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.copy) },
                                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                                            enabled = activeTab != null,
                                            onClick = {
                                                moreMenuOpen = false
                                                activeTab?.let { tab ->
                                                    val sel = tab.textFieldValue.selection
                                                    val textToCopy = if (!sel.collapsed) {
                                                        tab.textFieldValue.text.substring(
                                                            minOf(sel.start, sel.end),
                                                            maxOf(sel.start, sel.end)
                                                        )
                                                    } else tab.textFieldValue.text
                                                    clipboard.setText(AnnotatedString(textToCopy))
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.cut) },
                                            leadingIcon = { Icon(Icons.Rounded.ContentCut, null) },
                                            enabled = activeTab != null && !activeTab.textFieldValue.selection.collapsed,
                                            onClick = {
                                                moreMenuOpen = false
                                                activeTab?.let { tab ->
                                                    val sel = tab.textFieldValue.selection
                                                    val start = minOf(sel.start, sel.end)
                                                    val end = maxOf(sel.start, sel.end)
                                                    val cutText = tab.textFieldValue.text.substring(start, end)
                                                    clipboard.setText(AnnotatedString(cutText))
                                                    val updated = tab.textFieldValue.text.removeRange(start, end)
                                                    viewModel.onEditorValueChange(
                                                        TextFieldValue(updated, TextRange(start))
                                                    )
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.paste) },
                                            leadingIcon = { Icon(Icons.Rounded.ContentPaste, null) },
                                            enabled = activeTab != null,
                                            onClick = {
                                                moreMenuOpen = false
                                                val clip = clipboard.getText()?.text
                                                if (!clip.isNullOrEmpty() && activeTab != null) {
                                                    viewModel.insertSymbol(clip)
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.exportZip) },
                                            leadingIcon = { Icon(Icons.Rounded.FolderZip, null) },
                                            onClick = {
                                                moreMenuOpen = false
                                                val safeName = (state.project?.name ?: "project")
                                                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                                exportZipLauncher.launch("${safeName.ifBlank { "project" }}.zip")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.settings) },
                                            leadingIcon = { Icon(Icons.Rounded.Settings, null) },
                                            onClick = {
                                                moreMenuOpen = false
                                                onOpenSettings()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isTabletSplit) {
                            explorerContent(Modifier.width(290.dp))
                        }

                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AnimatedVisibility(visible = state.recoveredDraftNotice) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.draftRecovered,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = viewModel::dismissDraftNotice) {
                                            Text(strings.confirm)
                                        }
                                    }
                                }
                            }

                            // Open File Tabs Strip
                            if (state.openTabs.isNotEmpty()) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(state.openTabs, key = { it.relativePath }) { tab ->
                                            val isActive = tab.relativePath == state.activeTabPath
                                            val visual = fileVisualFor(tab.fileName, isDirectory = false)
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isActive) {
                                                    tokens.glassCardElevated
                                                } else {
                                                    tokens.glassCard.copy(alpha = 0.65f)
                                                },
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = if (isActive) {
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                    } else {
                                                        tokens.glassBorderEnd
                                                    }
                                                ),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { viewModel.selectTab(tab.relativePath) }
                                                    .testTag("editor_tab_${tab.relativePath}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = visual.icon,
                                                        contentDescription = visual.badge,
                                                        tint = visual.color,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = tab.fileName,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    if (tab.isDirty) {
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .size(7.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.primary)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.closeTab(tab.relativePath) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Close,
                                                            contentDescription = strings.closeTab,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Main Editor Canvas or Empty State
                            val activeTab = state.activeTab
                            when {
                                state.isLoading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                                activeTab == null -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LiquidGlassCard(
                                            contentPadding = PaddingValues(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Code,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = strings.noOpenFilesTitle,
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = strings.noOpenFilesSubtitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(onClick = { createFileParent = "" }) {
                                                    Text(strings.newFile)
                                                }
                                                if (!isTabletSplit) {
                                                    TextButton(onClick = { scope.launch { drawerState.open() } }) {
                                                        Text(strings.explorer)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    CodeEditorView(
                                        tab = activeTab,
                                        settings = settings,
                                        completions = state.completions,
                                        isFindVisible = state.isFindBarVisible,
                                        findQuery = state.findQuery,
                                        replaceQuery = state.replaceQuery,
                                        matchCase = state.matchCase,
                                        findMatches = state.findMatches,
                                        activeFindIndex = state.activeFindIndex,
                                        bracketMatchPair = state.bracketMatchPair,
                                        onValueChange = viewModel::onEditorValueChange,
                                        onInsertSymbol = viewModel::insertSymbol,
                                        onApplyCompletion = viewModel::applyCompletion,
                                        onFindQueryChange = viewModel::updateFindQuery,
                                        onReplaceQueryChange = viewModel::updateReplaceQuery,
                                        onToggleMatchCase = viewModel::toggleMatchCase,
                                        onFindPrev = viewModel::findPrevious,
                                        onFindNext = viewModel::findNext,
                                        onReplaceOne = viewModel::replaceCurrentMatch,
                                        onReplaceAll = viewModel::replaceAllMatches,
                                        onCloseFind = viewModel::toggleFindBar,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isTabletSplit) {
            workspaceBody()
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = Color.Transparent,
                        modifier = Modifier.width(300.dp)
                    ) {
                        explorerContent(Modifier.fillMaxSize())
                    }
                },
                content = workspaceBody
            )
        }
    }

    // File Explorer Dialogs
    createFileParent?.let { parent ->
        TextInputDialog(
            title = strings.newFile,
            initialValue = "",
            label = strings.fileNameHint,
            confirmText = strings.create,
            onDismiss = { createFileParent = null },
            onConfirm = { input ->
                val fullRel = if (parent.isEmpty()) input else "$parent/$input"
                createFileParent = null
                viewModel.createFile(fullRel, strings.alreadyExistsError)
            }
        )
    }

    createFolderParent?.let { parent ->
        TextInputDialog(
            title = strings.newFolder,
            initialValue = "",
            label = strings.folderNameHint,
            confirmText = strings.create,
            onDismiss = { createFolderParent = null },
            onConfirm = { input ->
                val fullRel = if (parent.isEmpty()) input else "$parent/$input"
                createFolderParent = null
                viewModel.createFolder(fullRel, strings.alreadyExistsError)
            }
        )
    }

    itemToRename?.let { node ->
        TextInputDialog(
            title = strings.renameItem,
            initialValue = node.name,
            label = strings.fileNameHint,
            confirmText = strings.confirm,
            onDismiss = { itemToRename = null },
            onConfirm = { newName ->
                itemToRename = null
                viewModel.renameItem(node.relativePath, newName, strings.invalidNameError)
            }
        )
    }

    itemToMove?.let { node ->
        MoveItemDialog(
            sourcePath = node.relativePath,
            folders = state.folderPaths,
            onDismiss = { itemToMove = null },
            onSelectFolder = { targetFolder ->
                itemToMove = null
                viewModel.moveItem(node.relativePath, targetFolder, strings.operationFailed)
            }
        )
    }

    itemToDelete?.let { node ->
        ConfirmDeleteDialog(
            title = strings.deleteItemTitle,
            message = strings.deleteItemMessage(node.name),
            onDismiss = { itemToDelete = null },
            onConfirm = {
                itemToDelete = null
                viewModel.deleteItem(node.relativePath, strings.operationFailed)
            }
        )
    }
}
