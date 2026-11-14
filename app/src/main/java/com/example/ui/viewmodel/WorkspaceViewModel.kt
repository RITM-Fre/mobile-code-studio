package com.example.ui.viewmodel

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.ProjectEntity
import com.example.data.fs.FileNode
import com.example.data.fs.StorageStats
import com.example.data.prefs.AppSettings
import com.example.data.prefs.PreviewBehavior
import com.example.data.repository.IdeRepository
import com.example.editor.CodeCompletionEngine
import com.example.editor.CodeLanguage
import com.example.editor.CompletionSuggestion
import com.example.editor.EditorTextUtils
import com.example.server.LocalPreviewServerManager
import com.example.server.ServerStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorTabState(
    val relativePath: String,
    val fileName: String,
    val language: CodeLanguage,
    val textFieldValue: TextFieldValue,
    val savedText: String,
    val isDirty: Boolean = false,
    val undoStack: List<TextFieldValue> = emptyList(),
    val redoStack: List<TextFieldValue> = emptyList()
)

data class WorkspaceUiState(
    val project: ProjectEntity? = null,
    val fileTree: List<FileNode> = emptyList(),
    val folderPaths: List<String> = emptyList(),
    val fileSearchQuery: String = "",
    val expandedFolders: Set<String> = emptySet(),
    val openTabs: List<EditorTabState> = emptyList(),
    val activeTabPath: String? = null,
    val completions: List<CompletionSuggestion> = emptyList(),
    val isFindBarVisible: Boolean = false,
    val findQuery: String = "",
    val replaceQuery: String = "",
    val matchCase: Boolean = false,
    val findMatches: List<IntRange> = emptyList(),
    val activeFindIndex: Int = -1,
    val bracketMatchPair: Pair<Int, Int>? = null,
    val isLoading: Boolean = true,
    val recoveredDraftNotice: Boolean = false
) {
    val activeTab: EditorTabState?
        get() = openTabs.firstOrNull { it.relativePath == activeTabPath }
}

sealed interface WorkspaceEvent {
    data class LaunchPreview(val previewUrl: String, val entryPath: String) : WorkspaceEvent
    data class ToastMessage(val message: String, val isError: Boolean = false) : WorkspaceEvent
}

class WorkspaceViewModel(
    private val repository: IdeRepository,
    private val serverManager: LocalPreviewServerManager,
    private val settingsFlow: StateFlow<AppSettings>
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    val serverStatus: StateFlow<ServerStatus> = serverManager.status

    private val _events = MutableSharedFlow<WorkspaceEvent>()
    val events = _events.asSharedFlow()

    private var loadedProjectId: String? = null
    private var draftSaveJob: Job? = null
    private var undoPushJob: Job? = null
    private var lastUndoBase: TextFieldValue? = null

    fun loadProjectIfNeeded(projectId: String, requestedFilePath: String? = null) {
        if (loadedProjectId == projectId && _uiState.value.project != null) {
            if (!requestedFilePath.isNullOrBlank()) {
                openFile(requestedFilePath)
            }
            return
        }
        loadedProjectId = projectId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, recoveredDraftNotice = false) }
            val project = repository.getProject(projectId)
            if (project == null) {
                _uiState.update { it.copy(isLoading = false, project = null) }
                return@launch
            }
            val tree = repository.getFileTree(project)
            val folders = repository.listFolders(project)
            val persistedTabs = repository.getOpenTabs(projectId)
            val allFiles = repository.listAllFiles(project)

            val tabsToOpen = persistedTabs.map { it.relativePath }.filter { it in allFiles }.toMutableList()
            if (!requestedFilePath.isNullOrBlank() && requestedFilePath in allFiles && requestedFilePath !in tabsToOpen) {
                tabsToOpen.add(requestedFilePath)
            }
            if (tabsToOpen.isEmpty()) {
                val defaultFile = allFiles.firstOrNull { it.equals("index.html", ignoreCase = true) }
                    ?: allFiles.firstOrNull()
                if (defaultFile != null) tabsToOpen.add(defaultFile)
            }

            val loadedTabs = mutableListOf<EditorTabState>()
            var anyRecovered = false
            for (path in tabsToOpen) {
                val res = repository.readFileOrDraft(project, path).getOrNull() ?: continue
                val (content, draft) = res
                if (draft != null) anyRecovered = true
                val cursor = (draft?.cursorPosition ?: 0).coerceIn(0, content.length)
                loadedTabs.add(
                    EditorTabState(
                        relativePath = path,
                        fileName = path.substringAfterLast('/'),
                        language = CodeLanguage.fromFileName(path),
                        textFieldValue = TextFieldValue(content, TextRange(cursor)),
                        savedText = if (draft != null) "" else content,
                        isDirty = draft != null
                    )
                )
            }

            val activePath = when {
                !requestedFilePath.isNullOrBlank() && loadedTabs.any { it.relativePath == requestedFilePath } -> requestedFilePath
                else -> persistedTabs.firstOrNull { it.isActive }?.relativePath
                    ?.takeIf { p -> loadedTabs.any { it.relativePath == p } }
                    ?: loadedTabs.firstOrNull()?.relativePath
            }

            _uiState.update {
                it.copy(
                    project = project,
                    fileTree = tree,
                    folderPaths = folders,
                    openTabs = loadedTabs,
                    activeTabPath = activePath,
                    isLoading = false,
                    recoveredDraftNotice = anyRecovered
                )
            }
            recomputeEditorDecorations()
        }
    }

    fun refreshTree() {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            val tree = repository.getFileTree(project)
            val folders = repository.listFolders(project)
            _uiState.update { it.copy(fileTree = tree, folderPaths = folders) }
        }
    }

    fun updateFileSearch(query: String) {
        _uiState.update { it.copy(fileSearchQuery = query) }
    }

    fun toggleFolderExpanded(relativePath: String) {
        _uiState.update { state ->
            val next = state.expandedFolders.toMutableSet()
            if (!next.add(relativePath)) next.remove(relativePath)
            state.copy(expandedFolders = next)
        }
    }

    fun openFile(relativePath: String) {
        val project = _uiState.value.project ?: return
        val existing = _uiState.value.openTabs.firstOrNull { it.relativePath == relativePath }
        if (existing != null) {
            _uiState.update { it.copy(activeTabPath = relativePath) }
            persistTabsAsync()
            recomputeEditorDecorations()
            return
        }

        viewModelScope.launch {
            repository.readFileOrDraft(project, relativePath).onSuccess { (content, draft) ->
                val cursor = (draft?.cursorPosition ?: 0).coerceIn(0, content.length)
                val newTab = EditorTabState(
                    relativePath = relativePath,
                    fileName = relativePath.substringAfterLast('/'),
                    language = CodeLanguage.fromFileName(relativePath),
                    textFieldValue = TextFieldValue(content, TextRange(cursor)),
                    savedText = if (draft != null) "" else content,
                    isDirty = draft != null
                )
                _uiState.update { state ->
                    state.copy(
                        openTabs = state.openTabs + newTab,
                        activeTabPath = relativePath,
                        recoveredDraftNotice = state.recoveredDraftNotice || draft != null
                    )
                }
                repository.recordRecentFile(project, relativePath)
                persistTabsAsync()
                recomputeEditorDecorations()
            }.onFailure { err ->
                _events.emit(WorkspaceEvent.ToastMessage(err.localizedMessage ?: "Failed to open file", isError = true))
            }
        }
    }

    fun selectTab(relativePath: String) {
        if (_uiState.value.activeTabPath == relativePath) return
        lastUndoBase = null
        _uiState.update { it.copy(activeTabPath = relativePath) }
        persistTabsAsync()
        recomputeEditorDecorations()
    }

    fun closeTab(relativePath: String) {
        val state = _uiState.value
        val idx = state.openTabs.indexOfFirst { it.relativePath == relativePath }
        if (idx < 0) return
        val tab = state.openTabs[idx]
        if (tab.isDirty && settingsFlow.value.autoSave) {
            saveTab(tab)
        }
        val remaining = state.openTabs.filterNot { it.relativePath == relativePath }
        val nextActive = if (state.activeTabPath == relativePath) {
            remaining.getOrNull(idx.coerceAtMost(remaining.lastIndex))?.relativePath
        } else {
            state.activeTabPath
        }
        _uiState.update {
            it.copy(openTabs = remaining, activeTabPath = nextActive)
        }
        persistTabsAsync()
        recomputeEditorDecorations()
    }

    fun onEditorValueChange(newValueRaw: TextFieldValue) {
        val state = _uiState.value
        val active = state.activeTab ?: return
        val oldVal = active.textFieldValue
        val processed = EditorTextUtils.applyAutoIndentIfNewline(oldVal, newValueRaw)
        val textChanged = processed.text != oldVal.text

        if (textChanged && lastUndoBase == null) {
            lastUndoBase = oldVal
        }

        val updatedTabs = state.openTabs.map { tab ->
            if (tab.relativePath == active.relativePath) {
                tab.copy(
                    textFieldValue = processed,
                    isDirty = processed.text != tab.savedText
                )
            } else tab
        }

        _uiState.update { it.copy(openTabs = updatedTabs) }
        recomputeEditorDecorations()

        if (textChanged) {
            scheduleUndoSnapshot(active.relativePath)
            scheduleDraftOrAutoSave(active.relativePath, processed)
        }
    }

    private fun scheduleUndoSnapshot(path: String) {
        undoPushJob?.cancel()
        undoPushJob = viewModelScope.launch {
            delay(450)
            val base = lastUndoBase ?: return@launch
            lastUndoBase = null
            _uiState.update { state ->
                val tabs = state.openTabs.map { tab ->
                    if (tab.relativePath == path) {
                        val nextUndo = (tab.undoStack + base).takeLast(50)
                        tab.copy(undoStack = nextUndo, redoStack = emptyList())
                    } else tab
                }
                state.copy(openTabs = tabs)
            }
        }
    }

    private fun scheduleDraftOrAutoSave(path: String, value: TextFieldValue) {
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(350)
            val project = _uiState.value.project ?: return@launch
            val currentTab = _uiState.value.openTabs.firstOrNull { it.relativePath == path } ?: return@launch
            if (settingsFlow.value.autoSave) {
                repository.saveFile(project, path, currentTab.textFieldValue.text).onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            openTabs = state.openTabs.map { t ->
                                if (t.relativePath == path) {
                                    t.copy(savedText = currentTab.textFieldValue.text, isDirty = false)
                                } else t
                            }
                        )
                    }
                }
            } else {
                repository.saveDraft(
                    projectId = project.id,
                    relativePath = path,
                    content = value.text,
                    cursorPosition = value.selection.start,
                    isDirty = currentTab.isDirty
                )
            }
        }
    }

    fun undo() {
        val active = _uiState.value.activeTab ?: return
        val snapshot = lastUndoBase ?: active.undoStack.lastOrNull() ?: return
        val updatedUndo = if (lastUndoBase != null) active.undoStack else active.undoStack.dropLast(1)
        lastUndoBase = null
        val updatedRedo = (active.redoStack + active.textFieldValue).takeLast(50)

        _uiState.update { state ->
            state.copy(
                openTabs = state.openTabs.map { t ->
                    if (t.relativePath == active.relativePath) {
                        t.copy(
                            textFieldValue = snapshot,
                            isDirty = snapshot.text != t.savedText,
                            undoStack = updatedUndo,
                            redoStack = updatedRedo
                        )
                    } else t
                }
            )
        }
        recomputeEditorDecorations()
    }

    fun redo() {
        val active = _uiState.value.activeTab ?: return
        val snapshot = active.redoStack.lastOrNull() ?: return
        val updatedRedo = active.redoStack.dropLast(1)
        val updatedUndo = (active.undoStack + active.textFieldValue).takeLast(50)

        _uiState.update { state ->
            state.copy(
                openTabs = state.openTabs.map { t ->
                    if (t.relativePath == active.relativePath) {
                        t.copy(
                            textFieldValue = snapshot,
                            isDirty = snapshot.text != t.savedText,
                            undoStack = updatedUndo,
                            redoStack = updatedRedo
                        )
                    } else t
                }
            )
        }
        recomputeEditorDecorations()
    }

    fun insertSymbol(symbol: String) {
        val active = _uiState.value.activeTab ?: return
        val nextValue = EditorTextUtils.insertSymbolAtCursor(active.textFieldValue, symbol)
        pushImmediateUndo(active.relativePath, active.textFieldValue)
        updateTabValueDirectly(active.relativePath, nextValue)
    }

    fun applyCompletion(suggestion: CompletionSuggestion) {
        val active = _uiState.value.activeTab ?: return
        val nextValue = EditorTextUtils.applyCompletion(active.textFieldValue, suggestion)
        pushImmediateUndo(active.relativePath, active.textFieldValue)
        updateTabValueDirectly(active.relativePath, nextValue)
    }

    fun selectAllText() {
        val active = _uiState.value.activeTab ?: return
        val updated = active.textFieldValue.copy(selection = TextRange(0, active.textFieldValue.text.length))
        updateTabValueDirectly(active.relativePath, updated, isTextEdit = false)
    }

    fun formatCurrentFileIndentation() {
        val active = _uiState.value.activeTab ?: return
        pushImmediateUndo(active.relativePath, active.textFieldValue)
        val formatted = EditorTextUtils.formatBasicIndentation(active.textFieldValue.text)
        val updated = TextFieldValue(formatted, TextRange(0))
        updateTabValueDirectly(active.relativePath, updated)
    }

    private fun pushImmediateUndo(path: String, snapshot: TextFieldValue) {
        lastUndoBase = null
        _uiState.update { state ->
            state.copy(
                openTabs = state.openTabs.map { t ->
                    if (t.relativePath == path) {
                        t.copy(undoStack = (t.undoStack + snapshot).takeLast(50), redoStack = emptyList())
                    } else t
                }
            )
        }
    }

    private fun updateTabValueDirectly(path: String, nextValue: TextFieldValue, isTextEdit: Boolean = true) {
        _uiState.update { state ->
            state.copy(
                openTabs = state.openTabs.map { t ->
                    if (t.relativePath == path) {
                        t.copy(
                            textFieldValue = nextValue,
                            isDirty = if (isTextEdit) nextValue.text != t.savedText else t.isDirty
                        )
                    } else t
                }
            )
        }
        recomputeEditorDecorations()
        if (isTextEdit) {
            scheduleDraftOrAutoSave(path, nextValue)
        }
    }

    fun toggleFindBar() {
        _uiState.update { state ->
            val next = !state.isFindBarVisible
            state.copy(isFindBarVisible = next)
        }
        recomputeEditorDecorations()
    }

    fun updateFindQuery(query: String) {
        _uiState.update { it.copy(findQuery = query, activeFindIndex = 0) }
        recomputeEditorDecorations()
    }

    fun updateReplaceQuery(query: String) {
        _uiState.update { it.copy(replaceQuery = query) }
    }

    fun toggleMatchCase() {
        _uiState.update { it.copy(matchCase = !it.matchCase) }
        recomputeEditorDecorations()
    }

    fun findNext() {
        val state = _uiState.value
        if (state.findMatches.isEmpty()) return
        val nextIdx = (state.activeFindIndex + 1) % state.findMatches.size
        jumpToFindMatch(nextIdx)
    }

    fun findPrevious() {
        val state = _uiState.value
        if (state.findMatches.isEmpty()) return
        val prevIdx = if (state.activeFindIndex <= 0) state.findMatches.lastIndex else state.activeFindIndex - 1
        jumpToFindMatch(prevIdx)
    }

    private fun jumpToFindMatch(index: Int) {
        val active = _uiState.value.activeTab ?: return
        val range = _uiState.value.findMatches.getOrNull(index) ?: return
        val updated = active.textFieldValue.copy(
            selection = TextRange(range.first, range.last + 1)
        )
        _uiState.update { state ->
            state.copy(
                activeFindIndex = index,
                openTabs = state.openTabs.map { t ->
                    if (t.relativePath == active.relativePath) t.copy(textFieldValue = updated) else t
                }
            )
        }
    }

    fun replaceCurrentMatch() {
        val state = _uiState.value
        val active = state.activeTab ?: return
        val range = state.findMatches.getOrNull(state.activeFindIndex) ?: return
        pushImmediateUndo(active.relativePath, active.textFieldValue)
        val text = active.textFieldValue.text
        val replacement = state.replaceQuery
        val updatedText = text.substring(0, range.first) + replacement + text.substring(range.last + 1)
        val newCursor = range.first + replacement.length
        updateTabValueDirectly(active.relativePath, TextFieldValue(updatedText, TextRange(newCursor)))
    }

    fun replaceAllMatches() {
        val state = _uiState.value
        val active = state.activeTab ?: return
        if (state.findQuery.isEmpty() || state.findMatches.isEmpty()) return
        pushImmediateUndo(active.relativePath, active.textFieldValue)
        val updatedText = active.textFieldValue.text.replace(
            oldValue = state.findQuery,
            newValue = state.replaceQuery,
            ignoreCase = !state.matchCase
        )
        updateTabValueDirectly(active.relativePath, TextFieldValue(updatedText, TextRange(0)))
    }

    private fun recomputeEditorDecorations() {
        val state = _uiState.value
        val active = state.activeTab
        if (active == null) {
            _uiState.update {
                it.copy(completions = emptyList(), findMatches = emptyList(), activeFindIndex = -1, bracketMatchPair = null)
            }
            return
        }
        val text = active.textFieldValue.text
        val cursor = active.textFieldValue.selection.start
        val completions = CodeCompletionEngine.suggest(text, cursor, active.language)
        val bracketPair = EditorTextUtils.findMatchingBracket(text, cursor)
        val matches = if (state.isFindBarVisible && state.findQuery.isNotEmpty()) {
            EditorTextUtils.findAllMatches(text, state.findQuery, state.matchCase)
        } else {
            emptyList()
        }
        val clampedFindIndex = if (matches.isEmpty()) -1 else state.activeFindIndex.coerceIn(0, matches.lastIndex)

        _uiState.update {
            it.copy(
                completions = completions,
                bracketMatchPair = bracketPair,
                findMatches = matches,
                activeFindIndex = clampedFindIndex
            )
        }
    }

    fun saveActiveFile(savedLabel: String) {
        val active = _uiState.value.activeTab ?: return
        saveTab(active, toastMessage = savedLabel)
    }

    suspend fun saveAllDirtyFiles(): Boolean {
        val project = _uiState.value.project ?: return false
        var allOk = true
        _uiState.value.openTabs.filter { it.isDirty }.forEach { tab ->
            val res = repository.saveFile(project, tab.relativePath, tab.textFieldValue.text)
            if (res.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        openTabs = state.openTabs.map { t ->
                            if (t.relativePath == tab.relativePath) {
                                t.copy(savedText = tab.textFieldValue.text, isDirty = false)
                            } else t
                        }
                    )
                }
            } else {
                allOk = false
            }
        }
        return allOk
    }

    private fun saveTab(tab: EditorTabState, toastMessage: String? = null) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            repository.saveFile(project, tab.relativePath, tab.textFieldValue.text)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            openTabs = state.openTabs.map { t ->
                                if (t.relativePath == tab.relativePath) {
                                    t.copy(savedText = tab.textFieldValue.text, isDirty = false)
                                } else t
                            }
                        )
                    }
                    if (toastMessage != null) {
                        _events.emit(WorkspaceEvent.ToastMessage(toastMessage))
                    }
                }
                .onFailure { err ->
                    _events.emit(WorkspaceEvent.ToastMessage(err.localizedMessage ?: "Save failed", isError = true))
                }
        }
    }

    fun launchPreview(forceProjectEntry: Boolean? = null, noHtmlError: String) {
        val state = _uiState.value
        val project = state.project ?: return
        viewModelScope.launch {
            // Flush unsaved edits to disk so local HTTP server serves latest code immediately
            saveAllDirtyFiles()
            val allFiles = repository.listAllFiles(project)
            val activeRel = state.activeTab?.relativePath
            val activeIsHtml = activeRel != null &&
                (activeRel.endsWith(".html", ignoreCase = true) || activeRel.endsWith(".htm", ignoreCase = true))

            val useProjectEntry = forceProjectEntry ?: (settingsFlow.value.previewBehavior == PreviewBehavior.PROJECT_ENTRY)

            val entryPath = when {
                !useProjectEntry && activeIsHtml -> activeRel!!
                allFiles.any { it.equals("index.html", ignoreCase = true) } ->
                    allFiles.first { it.equals("index.html", ignoreCase = true) }
                activeIsHtml -> activeRel!!
                allFiles.any { it.endsWith(".html", ignoreCase = true) || it.endsWith(".htm", ignoreCase = true) } ->
                    allFiles.first { it.endsWith(".html", ignoreCase = true) || it.endsWith(".htm", ignoreCase = true) }
                else -> {
                    _events.emit(WorkspaceEvent.ToastMessage(noHtmlError, isError = true))
                    return@launch
                }
            }

            val rootDir = repository.getProjectRootDir(project)
            val port = settingsFlow.value.serverPort
            val res = serverManager.startForProject(project.id, rootDir, port)
            res.onSuccess { running ->
                val url = serverManager.buildPreviewUrl(running.baseUrl, entryPath)
                _events.emit(WorkspaceEvent.LaunchPreview(url, entryPath))
            }.onFailure { err ->
                _events.emit(
                    WorkspaceEvent.ToastMessage(
                        err.localizedMessage ?: "Could not start local server on port $port",
                        isError = true
                    )
                )
            }
        }
    }

    fun stopLocalServer() {
        serverManager.stopServer()
    }

    fun createFile(relativePath: String, errorMsg: String) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            repository.createFile(project, relativePath).onSuccess { createdPath ->
                refreshTree()
                openFile(createdPath)
            }.onFailure {
                _events.emit(WorkspaceEvent.ToastMessage(it.localizedMessage ?: errorMsg, isError = true))
            }
        }
    }

    fun createFolder(relativePath: String, errorMsg: String) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            repository.createFolder(project, relativePath).onSuccess { createdPath ->
                _uiState.update { it.copy(expandedFolders = it.expandedFolders + createdPath) }
                refreshTree()
            }.onFailure {
                _events.emit(WorkspaceEvent.ToastMessage(it.localizedMessage ?: errorMsg, isError = true))
            }
        }
    }

    fun renameItem(oldPath: String, newName: String, errorMsg: String) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            repository.renameItem(project, oldPath, newName).onSuccess { newPath ->
                _uiState.update { state ->
                    val updatedTabs = state.openTabs.map { tab ->
                        when {
                            tab.relativePath == oldPath -> tab.copy(
                                relativePath = newPath,
                                fileName = newPath.substringAfterLast('/'),
                                language = CodeLanguage.fromFileName(newPath)
                            )
                            tab.relativePath.startsWith("$oldPath/") -> {
                                val remapped = newPath + tab.relativePath.removePrefix(oldPath)
                                tab.copy(relativePath = remapped, fileName = remapped.substringAfterLast('/'))
                            }
                            else -> tab
                        }
                    }
                    val nextActive = when {
                        state.activeTabPath == oldPath -> newPath
                        state.activeTabPath?.startsWith("$oldPath/") == true ->
                            newPath + state.activeTabPath.removePrefix(oldPath)
                        else -> state.activeTabPath
                    }
                    state.copy(openTabs = updatedTabs, activeTabPath = nextActive)
                }
                persistTabsAsync()
                refreshTree()
            }.onFailure {
                _events.emit(WorkspaceEvent.ToastMessage(it.localizedMessage ?: errorMsg, isError = true))
            }
        }
    }

    fun duplicateItem(relativePath: String, errorMsg: String) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            repository.duplicateItem(project, relativePath).onSuccess {
                refreshTree()
            }.onFailure {
                _events.emit(WorkspaceEvent.ToastMessage(it.localizedMessage ?: errorMsg, isError = true))
            }
        }
    }

    fun moveItem(sourcePath: String, targetFolder: String, errorMsg: String) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            repository.moveItem(project, sourcePath, targetFolder).onSuccess { newPath ->
                _uiState.update { state ->
                    val updatedTabs = state.openTabs.map { tab ->
                        if (tab.relativePath == sourcePath) {
                            tab.copy(relativePath = newPath, fileName = newPath.substringAfterLast('/'))
                        } else if (tab.relativePath.startsWith("$sourcePath/")) {
                            val remapped = newPath + tab.relativePath.removePrefix(sourcePath)
                            tab.copy(relativePath = remapped, fileName = remapped.substringAfterLast('/'))
                        } else tab
                    }
                    val nextActive = when {
                        state.activeTabPath == sourcePath -> newPath
                        state.activeTabPath?.startsWith("$sourcePath/") == true ->
                            newPath + state.activeTabPath.removePrefix(sourcePath)
                        else -> state.activeTabPath
                    }
                    state.copy(openTabs = updatedTabs, activeTabPath = nextActive)
                }
                persistTabsAsync()
                refreshTree()
            }.onFailure {
                _events.emit(WorkspaceEvent.ToastMessage(it.localizedMessage ?: errorMsg, isError = true))
            }
        }
    }

    fun deleteItem(relativePath: String, errorMsg: String) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            repository.deleteItem(project, relativePath).onSuccess {
                _uiState.update { state ->
                    val remaining = state.openTabs.filterNot {
                        it.relativePath == relativePath || it.relativePath.startsWith("$relativePath/")
                    }
                    val nextActive = if (state.activeTabPath == relativePath ||
                        state.activeTabPath?.startsWith("$relativePath/") == true
                    ) {
                        remaining.firstOrNull()?.relativePath
                    } else state.activeTabPath
                    state.copy(openTabs = remaining, activeTabPath = nextActive)
                }
                persistTabsAsync()
                refreshTree()
            }.onFailure {
                _events.emit(WorkspaceEvent.ToastMessage(it.localizedMessage ?: errorMsg, isError = true))
            }
        }
    }

    fun exportProjectZip(uri: Uri, successMsg: String, failMsg: String) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            saveAllDirtyFiles()
            repository.exportProjectZip(project, uri)
                .onSuccess { _events.emit(WorkspaceEvent.ToastMessage(successMsg)) }
                .onFailure { _events.emit(WorkspaceEvent.ToastMessage(failMsg, isError = true)) }
        }
    }

    fun dismissDraftNotice() {
        _uiState.update { it.copy(recoveredDraftNotice = false) }
    }

    private fun persistTabsAsync() {
        val project = _uiState.value.project ?: return
        val tabPaths = _uiState.value.openTabs.map { it.relativePath }
        val activePath = _uiState.value.activeTabPath
        viewModelScope.launch {
            repository.persistOpenTabs(project.id, tabPaths, activePath)
        }
    }

    class Factory(
        private val repository: IdeRepository,
        private val serverManager: LocalPreviewServerManager,
        private val settingsFlow: StateFlow<AppSettings>
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WorkspaceViewModel(repository, serverManager, settingsFlow) as T
    }
}
