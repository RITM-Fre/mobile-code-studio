package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.fs.FileNode
import com.example.ui.components.fileVisualFor
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.LocalStrings

private data class FlatTreeRow(
    val node: FileNode,
    val depth: Int
)

@Composable
fun FileExplorerPanel(
    projectName: String,
    tree: List<FileNode>,
    expandedFolders: Set<String>,
    activeFilePath: String?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onToggleFolder: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onPreviewHtmlFile: (String) -> Unit,
    onCreateFileRequest: (parentFolder: String) -> Unit,
    onCreateFolderRequest: (parentFolder: String) -> Unit,
    onRenameRequest: (FileNode) -> Unit,
    onDuplicateRequest: (FileNode) -> Unit,
    onMoveRequest: (FileNode) -> Unit,
    onDeleteRequest: (FileNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val tokens = LocalGlassTokens.current

    val rows = remember(tree, expandedFolders, searchQuery) {
        buildFlatRows(tree, expandedFolders, searchQuery.trim())
    }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = tokens.glassCardElevated,
        shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxHeight().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.explorer,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { onCreateFileRequest("") },
                    modifier = Modifier.testTag("explorer_new_file_button")
                ) {
                    Icon(Icons.Rounded.NoteAdd, contentDescription = strings.newFile)
                }
                IconButton(
                    onClick = { onCreateFolderRequest("") },
                    modifier = Modifier.testTag("explorer_new_folder_button")
                ) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = strings.newFolder)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(strings.searchFilesHint, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, null, Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explorer_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(rows, key = { it.node.relativePath }) { row ->
                    FileTreeRowItem(
                        node = row.node,
                        depth = row.depth,
                        isExpanded = row.node.relativePath in expandedFolders,
                        isSelected = row.node.relativePath == activeFilePath,
                        onClick = {
                            if (row.node.isDirectory) {
                                onToggleFolder(row.node.relativePath)
                            } else {
                                onOpenFile(row.node.relativePath)
                            }
                        },
                        onCreateFileInside = { onCreateFileRequest(row.node.relativePath) },
                        onCreateFolderInside = { onCreateFolderRequest(row.node.relativePath) },
                        onPreviewHtml = { onPreviewHtmlFile(row.node.relativePath) },
                        onRename = { onRenameRequest(row.node) },
                        onDuplicate = { onDuplicateRequest(row.node) },
                        onMove = { onMoveRequest(row.node) },
                        onDelete = { onDeleteRequest(row.node) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileTreeRowItem(
    node: FileNode,
    depth: Int,
    isExpanded: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCreateFileInside: () -> Unit,
    onCreateFolderInside: () -> Unit,
    onPreviewHtml: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalStrings.current
    val visual = fileVisualFor(node.name, node.isDirectory, isExpanded)
    var menuOpen by remember { mutableStateOf(false) }
    val isHtml = !node.isDirectory && (node.extension == "html" || node.extension == "htm")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                )
                .clickable(onClick = onClick)
                .padding(start = (8 + depth * 14).dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                .testTag("tree_item_${node.relativePath}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.isDirectory) {
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = visual.icon,
                contentDescription = visual.badge,
                tint = visual.color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = strings.moreOptions,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    if (node.isDirectory) {
                        DropdownMenuItem(
                            text = { Text(strings.newFile) },
                            leadingIcon = { Icon(Icons.Rounded.NoteAdd, null) },
                            onClick = {
                                menuOpen = false
                                onCreateFileInside()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.newFolder) },
                            leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, null) },
                            onClick = {
                                menuOpen = false
                                onCreateFolderInside()
                            }
                        )
                    } else if (isHtml) {
                        DropdownMenuItem(
                            text = { Text(strings.runThisFile) },
                            leadingIcon = { Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                menuOpen = false
                                onPreviewHtml()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(strings.rename) },
                        leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.duplicate) },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                        onClick = {
                            menuOpen = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.move) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.DriveFileMove, null) },
                        onClick = {
                            menuOpen = false
                            onMove()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.delete, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

private fun buildFlatRows(
    nodes: List<FileNode>,
    expanded: Set<String>,
    query: String,
    depth: Int = 0
): List<FlatTreeRow> {
    if (query.isNotEmpty()) {
        val matches = mutableListOf<FlatTreeRow>()
        fun collect(list: List<FileNode>) {
            list.forEach { n ->
                if (!n.isDirectory && n.relativePath.contains(query, ignoreCase = true)) {
                    matches.add(FlatTreeRow(n, 0))
                }
                if (n.isDirectory) collect(n.children)
            }
        }
        collect(nodes)
        return matches
    }
    val out = mutableListOf<FlatTreeRow>()
    nodes.forEach { node ->
        out.add(FlatTreeRow(node, depth))
        if (node.isDirectory && node.relativePath in expanded) {
            out.addAll(buildFlatRows(node.children, expanded, "", depth + 1))
        }
    }
    return out
}
