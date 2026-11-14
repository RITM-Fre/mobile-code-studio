package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.AppLanguage
import com.example.data.db.ProjectEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CreateProjectDialog
import com.example.ui.components.FloatingIslandHeader
import com.example.ui.components.GlassActionPill
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.ProjectCard
import com.example.ui.components.TextInputDialog
import com.example.ui.components.fileVisualFor
import com.example.ui.components.formatRelativeDate
import com.example.ui.theme.LocalStrings
import com.example.ui.viewmodel.DashboardEvent
import com.example.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    currentLanguage: AppLanguage,
    onToggleLanguage: () -> Unit,
    onOpenProject: (projectId: String, initialFile: String?) -> Unit,
    onNavigateAllProjects: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateAbout: () -> Unit
) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val isCompactHeader by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 36
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var projectToRename by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectToExport by remember { mutableStateOf<ProjectEntity?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }

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
                is DashboardEvent.OpenProject -> onOpenProject(event.projectId, event.initialFile)
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
                    title = strings.appName,
                    subtitle = strings.dashboard,
                    isCompact = isCompactHeader,
                    actions = {
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onToggleLanguage)
                                .testTag("dashboard_language_toggle"),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = strings.languageTitle,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentLanguage == AppLanguage.PERSIAN) "FA" else "EN",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(
                            onClick = onNavigateAbout,
                            modifier = Modifier.testTag("dashboard_about_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = strings.about,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onNavigateSettings,
                            modifier = Modifier.testTag("dashboard_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = strings.settings,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
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
                        .widthIn(max = 860.dp)
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Welcome Hero Card
                    item {
                        LiquidGlassCard(
                            elevated = true,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_liquid_glass_hero),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .alpha(0.35f)
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                                                )
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = strings.welcomeTitle,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = strings.welcomeSubtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { showCreateDialog = true },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.testTag("dashboard_create_project_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(strings.createNewProject)
                                        }
                                        GlassActionPill(
                                            label = strings.allProjects,
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.FolderCopy,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            onClick = onNavigateAllProjects,
                                            modifier = Modifier.testTag("dashboard_all_projects_button")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick Actions Row
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = strings.quickActions,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    QuickActionTile(
                                        icon = Icons.Rounded.Add,
                                        label = strings.createNewProject,
                                        accent = MaterialTheme.colorScheme.primary,
                                        onClick = { showCreateDialog = true }
                                    )
                                }
                                item {
                                    QuickActionTile(
                                        icon = Icons.Rounded.FolderZip,
                                        label = strings.importZip,
                                        accent = MaterialTheme.colorScheme.secondary,
                                        onClick = { importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed")) }
                                    )
                                }
                                item {
                                    QuickActionTile(
                                        icon = Icons.Rounded.FolderCopy,
                                        label = strings.allProjects,
                                        accent = MaterialTheme.colorScheme.tertiary,
                                        onClick = onNavigateAllProjects
                                    )
                                }
                                item {
                                    QuickActionTile(
                                        icon = Icons.Rounded.Settings,
                                        label = strings.settings,
                                        accent = MaterialTheme.colorScheme.primary,
                                        onClick = onNavigateSettings
                                    )
                                }
                            }
                        }
                    }

                    // Recent Projects Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.recentProjects,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (state.projects.isNotEmpty()) {
                                TextButton(onClick = onNavigateAllProjects) {
                                    Text(strings.allProjects)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (state.projects.isEmpty()) {
                        item {
                            LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_empty_workspace),
                                        contentDescription = strings.emptyProjectsTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(170.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    )
                                    Text(
                                        text = strings.emptyProjectsTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = strings.emptyProjectsSubtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { showCreateDialog = true },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.testTag("empty_create_project_button")
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.createNewProject)
                                    }
                                }
                            }
                        }
                    } else {
                        items(
                            items = state.projects.take(5),
                            key = { it.id }
                        ) { project ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically { it / 4 }
                            ) {
                                ProjectCard(
                                    project = project,
                                    onOpen = { onOpenProject(project.id, null) },
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

                    // Recently Opened Files Section
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = strings.recentFiles,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            if (state.recentFiles.isEmpty()) {
                                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = strings.emptyRecentFiles,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.recentFiles.take(6).forEach { recent ->
                                        val visual = fileVisualFor(recent.fileName, isDirectory = false)
                                        LiquidGlassCard(
                                            onClick = { onOpenProject(recent.projectId, recent.relativePath) },
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(visual.color.copy(alpha = 0.16f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = visual.icon,
                                                        contentDescription = visual.badge,
                                                        tint = visual.color,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = recent.fileName,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${recent.projectName} • ${recent.relativePath}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = formatRelativeDate(recent.openedAt),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
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

    pendingImportUri?.let { uri ->
        TextInputDialog(
            title = strings.importZip,
            initialValue = "Imported Web Project",
            label = strings.projectName,
            confirmText = strings.create,
            onDismiss = { pendingImportUri = null },
            onConfirm = { name ->
                pendingImportUri = null
                viewModel.importProjectZip(uri, name, strings.projectImported, strings.operationFailed)
            }
        )
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        modifier = Modifier.widthIn(min = 128.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
