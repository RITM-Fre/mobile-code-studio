package com.example.data.repository

import android.net.Uri
import com.example.core.CustomStarterFile
import com.example.core.ProjectTemplateId
import com.example.core.ProjectTemplates
import com.example.data.db.EditorDraftEntity
import com.example.data.db.IdeDao
import com.example.data.db.OpenTabEntity
import com.example.data.db.ProjectEntity
import com.example.data.db.RecentFileEntity
import com.example.data.fs.FileNode
import com.example.data.fs.ProjectStorageManager
import com.example.data.fs.StorageStats
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

class IdeRepository(
    private val dao: IdeDao,
    private val storage: ProjectStorageManager
) {
    val projects: Flow<List<ProjectEntity>> = dao.observeProjects()
    val recentFiles: Flow<List<RecentFileEntity>> = dao.observeRecentFiles()

    suspend fun getProject(projectId: String): ProjectEntity? = dao.getProjectById(projectId)

    fun getProjectRootDir(project: ProjectEntity): File =
        storage.getProjectRoot(project.rootFolderName)

    suspend fun createProject(
        name: String,
        description: String,
        templateId: ProjectTemplateId,
        customSelections: Set<CustomStarterFile> = emptySet(),
        accentIndex: Int = (0..4).random()
    ): Result<ProjectEntity> = runCatching {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Project name cannot be empty" }
        val id = UUID.randomUUID().toString()
        val folderName = "proj_${id.take(12)}"
        val starterFiles = ProjectTemplates.generateStarterFiles(
            templateId = templateId,
            projectName = cleanName,
            description = description.trim(),
            customSelections = customSelections
        )
        val count = storage.createProjectFiles(folderName, starterFiles)
        val entryFile = when {
            starterFiles.containsKey("index.html") -> "index.html"
            starterFiles.keys.any { it.endsWith(".html") || it.endsWith(".htm") } ->
                starterFiles.keys.first { it.endsWith(".html") || it.endsWith(".htm") }
            else -> "index.html"
        }
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            id = id,
            name = cleanName,
            description = description.trim(),
            templateId = templateId.id,
            rootFolderName = folderName,
            entryFile = entryFile,
            fileCount = count,
            createdAt = now,
            updatedAt = now,
            accentIndex = accentIndex
        )
        dao.upsertProject(project)
        if (starterFiles.containsKey("index.html")) {
            dao.upsertOpenTabs(
                listOf(
                    OpenTabEntity(
                        id = "$id::index.html",
                        projectId = id,
                        relativePath = "index.html",
                        sortOrder = 0,
                        isActive = true
                    )
                )
            )
        }
        project
    }

    suspend fun renameProject(projectId: String, newName: String): Result<Unit> = runCatching {
        val clean = newName.trim()
        require(clean.isNotEmpty()) { "Project name cannot be empty" }
        val existing = dao.getProjectById(projectId) ?: error("Project not found")
        dao.upsertProject(
            existing.copy(
                name = clean,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun duplicateProject(projectId: String, copySuffix: String): Result<ProjectEntity> =
        runCatching {
            val source = dao.getProjectById(projectId) ?: error("Project not found")
            val newId = UUID.randomUUID().toString()
            val newFolder = "proj_${newId.take(12)}"
            val count = storage.copyProjectDirectory(source.rootFolderName, newFolder)
            val now = System.currentTimeMillis()
            val copy = source.copy(
                id = newId,
                name = "${source.name} ($copySuffix)",
                rootFolderName = newFolder,
                fileCount = count,
                createdAt = now,
                updatedAt = now
            )
            dao.upsertProject(copy)
            copy
        }

    suspend fun deleteProject(projectId: String): Result<Unit> = runCatching {
        val existing = dao.getProjectById(projectId) ?: return@runCatching
        storage.deleteProjectDirectory(existing.rootFolderName)
        dao.deleteRecentFilesForProject(projectId)
        dao.clearOpenTabsForProject(projectId)
        dao.deleteDraftsForProject(projectId)
        dao.deleteProject(projectId)
    }

    suspend fun refreshProjectStats(projectId: String) {
        val existing = dao.getProjectById(projectId) ?: return
        val count = storage.countFiles(existing.rootFolderName)
        dao.upsertProject(
            existing.copy(
                fileCount = count,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getFileTree(project: ProjectEntity): List<FileNode> =
        storage.buildFileTree(project.rootFolderName)

    suspend fun listFolders(project: ProjectEntity): List<String> =
        storage.listFolderPaths(project.rootFolderName)

    suspend fun listAllFiles(project: ProjectEntity): List<String> =
        storage.listAllFilePaths(project.rootFolderName)

    suspend fun readFileOrDraft(project: ProjectEntity, relativePath: String): Result<Pair<String, EditorDraftEntity?>> {
        val diskResult = storage.readFileText(project.rootFolderName, relativePath)
        val draft = dao.getDraft(project.id, relativePath)
        return diskResult.map { diskContent ->
            if (draft != null && draft.isDirty) {
                draft.content to draft
            } else {
                diskContent to null
            }
        }
    }

    suspend fun saveFile(project: ProjectEntity, relativePath: String, content: String): Result<Unit> {
        val res = storage.saveFileText(project.rootFolderName, relativePath, content)
        if (res.isSuccess) {
            dao.deleteDraft(project.id, relativePath)
            refreshProjectStats(project.id)
        }
        return res
    }

    suspend fun saveDraft(
        projectId: String,
        relativePath: String,
        content: String,
        cursorPosition: Int,
        isDirty: Boolean
    ) {
        dao.upsertDraft(
            EditorDraftEntity(
                id = "$projectId::$relativePath",
                projectId = projectId,
                relativePath = relativePath,
                content = content,
                cursorPosition = cursorPosition,
                isDirty = isDirty,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getDirtyDrafts(projectId: String): List<EditorDraftEntity> =
        dao.getDirtyDrafts(projectId)

    suspend fun recordRecentFile(project: ProjectEntity, relativePath: String) {
        val fileName = relativePath.substringAfterLast('/')
        dao.upsertRecentFile(
            RecentFileEntity(
                id = "${project.id}::$relativePath",
                projectId = project.id,
                projectName = project.name,
                relativePath = relativePath,
                fileName = fileName,
                openedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getOpenTabs(projectId: String): List<OpenTabEntity> = dao.getOpenTabs(projectId)

    suspend fun persistOpenTabs(projectId: String, tabPaths: List<String>, activePath: String?) {
        dao.clearOpenTabsForProject(projectId)
        val entities = tabPaths.mapIndexed { index, path ->
            OpenTabEntity(
                id = "$projectId::$path",
                projectId = projectId,
                relativePath = path,
                sortOrder = index,
                isActive = path == activePath
            )
        }
        if (entities.isNotEmpty()) {
            dao.upsertOpenTabs(entities)
        }
    }

    suspend fun createFile(project: ProjectEntity, relativePath: String): Result<String> {
        val res = storage.createFile(project.rootFolderName, relativePath, defaultStarterForExtension(relativePath))
        if (res.isSuccess) refreshProjectStats(project.id)
        return res
    }

    suspend fun createFolder(project: ProjectEntity, relativePath: String): Result<String> {
        val res = storage.createFolder(project.rootFolderName, relativePath)
        if (res.isSuccess) refreshProjectStats(project.id)
        return res
    }

    suspend fun renameItem(project: ProjectEntity, oldPath: String, newName: String): Result<String> {
        val res = storage.renameItem(project.rootFolderName, oldPath, newName)
        if (res.isSuccess) {
            dao.deleteDraft(project.id, oldPath)
            dao.deleteRecentFile(project.id, oldPath)
            refreshProjectStats(project.id)
        }
        return res
    }

    suspend fun duplicateItem(project: ProjectEntity, relativePath: String): Result<String> {
        val res = storage.duplicateItem(project.rootFolderName, relativePath)
        if (res.isSuccess) refreshProjectStats(project.id)
        return res
    }

    suspend fun moveItem(project: ProjectEntity, sourcePath: String, targetFolder: String): Result<String> {
        val res = storage.moveItem(project.rootFolderName, sourcePath, targetFolder)
        if (res.isSuccess) {
            dao.deleteDraft(project.id, sourcePath)
            dao.deleteRecentFile(project.id, sourcePath)
            refreshProjectStats(project.id)
        }
        return res
    }

    suspend fun deleteItem(project: ProjectEntity, relativePath: String): Result<Unit> {
        val res = storage.deleteItem(project.rootFolderName, relativePath)
        if (res.isSuccess) {
            dao.deleteDraft(project.id, relativePath)
            dao.deleteRecentFile(project.id, relativePath)
            refreshProjectStats(project.id)
        }
        return res
    }

    suspend fun exportProjectZip(project: ProjectEntity, uri: Uri): Result<Unit> =
        storage.exportProjectToZip(project.rootFolderName, uri)

    suspend fun importProjectZip(
        uri: Uri,
        projectName: String,
        description: String = "Imported ZIP project"
    ): Result<ProjectEntity> = runCatching {
        val cleanName = projectName.trim().ifBlank { "Imported Project" }
        val id = UUID.randomUUID().toString()
        val folderName = "proj_${id.take(12)}"
        val count = storage.importZipToProject(uri, folderName).getOrThrow()
        val allFiles = storage.listAllFilePaths(folderName)
        val entry = allFiles.firstOrNull { it.equals("index.html", ignoreCase = true) }
            ?: allFiles.firstOrNull { it.endsWith(".html", ignoreCase = true) || it.endsWith(".htm", ignoreCase = true) }
            ?: "index.html"
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            id = id,
            name = cleanName,
            description = description,
            templateId = ProjectTemplateId.CUSTOM.id,
            rootFolderName = folderName,
            entryFile = entry,
            fileCount = count,
            createdAt = now,
            updatedAt = now,
            accentIndex = (0..4).random()
        )
        dao.upsertProject(project)
        project
    }

    suspend fun getStorageStats(projects: List<ProjectEntity>): StorageStats =
        storage.getStorageStats(projects.map { it.rootFolderName })

    suspend fun clearCache(): Boolean = storage.clearAppCache()

    private fun defaultStarterForExtension(relativePath: String): String {
        val ext = relativePath.substringAfterLast('.', "").lowercase()
        val base = relativePath.substringAfterLast('/').substringBeforeLast('.')
        return when (ext) {
            "html", "htm" -> """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>$base</title>
</head>
<body>
  <h1>$base</h1>
</body>
</html>
""".trimIndent()
            "css" -> "/* Styles for $base */\nbody {\n  margin: 0;\n}\n"
            "js" -> "// Script for $base\nconsole.log('$base loaded');\n"
            "json" -> "{\n  \"name\": \"$base\"\n}\n"
            "svg" -> "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\">\n  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"#38bdf8\" />\n</svg>\n"
            "md" -> "# $base\n\n"
            else -> ""
        }
    }
}
