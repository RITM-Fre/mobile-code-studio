package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.ProjectEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CreateProjectDialog
import com.example.ui.components.FloatingIslandHeader
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.ProjectCard
import com.example.ui.components.TextInputDialog
import com.example.ui.theme.LocalStrings
import com.example.ui.viewmodel.DashboardEvent
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.ProjectSortOrder

@Composable
fun ProjectsScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit
) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val isCompact by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 28 }
    }
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var projectToRename by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectToExport by remember { mutableStateOf<ProjectEntity?>(null) }

    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val target = projectToExport
        projectToExport = null
        if (uri != null && target != null) {
            viewModel.exportProjectZip(target, uri, strings.projectExported, strings.operationFailed)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DashboardEvent.OpenProject -> onOpenProject(event.projectId)
                is DashboardEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                FloatingIslandHeader(
                    title = strings.allProjects,
                    subtitle = "${state.projects.size}",
                    isCompact = isCompact,
                    leading = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("projects_back_button")) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("projects_fab_create"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = strings.createNewProject)
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 860.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        placeholder = { Text(strings.searchProjectsHint) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("projects_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SortByAlpha,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = state.sortOrder == ProjectSortOrder.UPDATED_DESC,
                            onClick = { viewModel.setSortOrder(ProjectSortOrder.UPDATED_DESC) },
                            label = { Text(strings.sortByDate) },
                            modifier = Modifier.testTag("sort_by_date_chip")
                        )
                        FilterChip(
                            selected = state.sortOrder == ProjectSortOrder.NAME_ASC,
                            onClick = { viewModel.setSortOrder(ProjectSortOrder.NAME_ASC) },
                            label = { Text(strings.sortByName) },
                            modifier = Modifier.testTag("sort_by_name_chip")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.filteredProjects.isEmpty()) {
                            item {
                                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = strings.emptyProjectsTitle,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = strings.emptyProjectsSubtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(state.filteredProjects, key = { it.id }) { project ->
                                ProjectCard(
                                    project = project,
                                    onOpen = { onOpenProject(project.id) },
                                    onRename = { projectToRename = project },
                                    onDuplicate = { viewModel.duplicateProject(project.id, strings.copySuffix) },
                                    onExportZip = {
                                        projectToExport = project
                                        val safeFile = project.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                        exportZipLauncher.launch("${safeFile.ifBlank { "project" }}.zip")
                                    },
                                    onDelete = { projectToDelete = project }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, template, custom ->
                showCreateDialog = false
                viewModel.createProject(name, desc, template, custom)
            }
        )
    }

    projectToRename?.let { target ->
        TextInputDialog(
            title = strings.renameProject,
            initialValue = target.name,
            label = strings.projectName,
            confirmText = strings.confirm,
            onDismiss = { projectToRename = null },
            onConfirm = { newName ->
                viewModel.renameProject(target.id, newName)
                projectToRename = null
            }
        )
    }

    projectToDelete?.let { target ->
        ConfirmDeleteDialog(
            title = strings.deleteProjectTitle,
            message = strings.deleteProjectMessage(target.name),
            onDismiss = { projectToDelete = null },
            onConfirm = {
                viewModel.deleteProject(target.id)
                projectToDelete = null
            }
        )
    }
}
