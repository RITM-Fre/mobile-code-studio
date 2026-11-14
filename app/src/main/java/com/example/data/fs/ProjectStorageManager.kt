package com.example.data.fs

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ProjectStorageManager(private val context: Context) {

    val projectsBaseDir: File
        get() = File(context.filesDir, "projects").apply { if (!exists()) mkdirs() }

    fun getProjectRoot(rootFolderName: String): File =
        File(projectsBaseDir, rootFolderName).apply { if (!exists()) mkdirs() }

    suspend fun createProjectFiles(
        rootFolderName: String,
        starterFiles: Map<String, String>
    ): Int = withContext(Dispatchers.IO) {
        val root = getProjectRoot(rootFolderName)
        starterFiles.forEach { (relPath, content) ->
            writeFileAtomic(root, relPath, content)
        }
        countFiles(root)
    }

    suspend fun copyProjectDirectory(
        sourceFolderName: String,
        destFolderName: String
    ): Int = withContext(Dispatchers.IO) {
        val src = getProjectRoot(sourceFolderName)
        val dst = getProjectRoot(destFolderName)
        if (src.exists()) {
            src.copyRecursively(dst, overwrite = true)
        }
        countFiles(dst)
    }

    suspend fun deleteProjectDirectory(rootFolderName: String): Boolean = withContext(Dispatchers.IO) {
        val dir = File(projectsBaseDir, rootFolderName)
        if (dir.exists()) dir.deleteRecursively() else true
    }

    suspend fun buildFileTree(rootFolderName: String): List<FileNode> = withContext(Dispatchers.IO) {
        val root = getProjectRoot(rootFolderName)
        buildChildrenNodes(root, "")
    }

    private fun buildChildrenNodes(dir: File, parentRelPath: String): List<FileNode> {
        val entries = dir.listFiles()?.toList().orEmpty()
            .filter { !it.name.endsWith(".mcs_tmp") }
            .sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) })

        return entries.map { file ->
            val relPath = if (parentRelPath.isEmpty()) file.name else "$parentRelPath/${file.name}"
            if (file.isDirectory) {
                FileNode(
                    name = file.name,
                    relativePath = relPath,
                    isDirectory = true,
                    extension = "",
                    sizeBytes = 0L,
                    lastModified = file.lastModified(),
                    children = buildChildrenNodes(file, relPath)
                )
            } else {
                FileNode(
                    name = file.name,
                    relativePath = relPath,
                    isDirectory = false,
                    extension = file.extension.lowercase(Locale.ROOT),
                    sizeBytes = file.length(),
                    lastModified = file.lastModified()
                )
            }
        }
    }

    suspend fun listFolderPaths(rootFolderName: String): List<String> = withContext(Dispatchers.IO) {
        val root = getProjectRoot(rootFolderName)
        val folders = mutableListOf("")
        fun walk(dir: File, rel: String) {
            dir.listFiles()?.sortedBy { it.name.lowercase(Locale.ROOT) }?.forEach { child ->
                if (child.isDirectory) {
                    val childRel = if (rel.isEmpty()) child.name else "$rel/${child.name}"
                    folders.add(childRel)
                    walk(child, childRel)
                }
            }
        }
        walk(root, "")
        folders
    }

    suspend fun listAllFilePaths(rootFolderName: String): List<String> = withContext(Dispatchers.IO) {
        val root = getProjectRoot(rootFolderName)
        val files = mutableListOf<String>()
        fun walk(dir: File, rel: String) {
            dir.listFiles()?.sortedBy { it.name.lowercase(Locale.ROOT) }?.forEach { child ->
                val childRel = if (rel.isEmpty()) child.name else "$rel/${child.name}"
                if (child.isDirectory) {
                    walk(child, childRel)
                } else if (!child.name.endsWith(".mcs_tmp")) {
                    files.add(childRel)
                }
            }
        }
        walk(root, "")
        files
    }

    suspend fun readFileText(rootFolderName: String, relativePath: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = getProjectRoot(rootFolderName)
                val target = resolveSafe(root, relativePath)
                if (!target.exists() || !target.isFile) {
                    error("File not found: $relativePath")
                }
                target.readText(StandardCharsets.UTF_8)
            }
        }

    suspend fun saveFileText(
        rootFolderName: String,
        relativePath: String,
        content: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val root = getProjectRoot(rootFolderName)
            writeFileAtomic(root, relativePath, content)
        }
    }

    private fun writeFileAtomic(root: File, relativePath: String, content: String) {
        val target = resolveSafe(root, relativePath)
        target.parentFile?.let { if (!it.exists()) it.mkdirs() }
        val tmp = File(target.parentFile, "${target.name}.mcs_tmp")
        FileOutputStream(tmp).use { fos ->
            fos.write(content.toByteArray(StandardCharsets.UTF_8))
            fos.fd.sync()
        }
        if (target.exists() && !target.delete()) {
            // Fallback copy if atomic rename cannot replace directly
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        } else if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    suspend fun createFile(rootFolderName: String, relativePath: String, initialContent: String = ""): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanPath = sanitizeRelativePath(relativePath)
                require(cleanPath.isNotEmpty()) { "Invalid file path" }
                val root = getProjectRoot(rootFolderName)
                val target = resolveSafe(root, cleanPath)
                require(!target.exists()) { "Item already exists" }
                writeFileAtomic(root, cleanPath, initialContent)
                cleanPath
            }
        }

    suspend fun createFolder(rootFolderName: String, relativePath: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanPath = sanitizeRelativePath(relativePath)
                require(cleanPath.isNotEmpty()) { "Invalid folder path" }
                val root = getProjectRoot(rootFolderName)
                val target = resolveSafe(root, cleanPath)
                require(!target.exists()) { "Folder already exists" }
                check(target.mkdirs()) { "Unable to create folder" }
                cleanPath
            }
        }

    suspend fun renameItem(
        rootFolderName: String,
        oldRelativePath: String,
        newName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitizedName = newName.trim()
            require(sanitizedName.isNotEmpty() && !sanitizedName.contains("/") && !sanitizedName.contains("\\") && sanitizedName != "." && sanitizedName != "..") {
                "Invalid name"
            }
            val root = getProjectRoot(rootFolderName)
            val src = resolveSafe(root, oldRelativePath)
            require(src.exists()) { "Source item does not exist" }
            val parentRel = oldRelativePath.substringBeforeLast('/', "")
            val newRelPath = if (parentRel.isEmpty()) sanitizedName else "$parentRel/$sanitizedName"
            val dst = resolveSafe(root, newRelPath)
            require(!dst.exists()) { "Destination already exists" }
            check(src.renameTo(dst)) { "Rename failed" }
            newRelPath
        }
    }

    suspend fun duplicateItem(
        rootFolderName: String,
        relativePath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val root = getProjectRoot(rootFolderName)
            val src = resolveSafe(root, relativePath)
            require(src.exists()) { "Source does not exist" }
            val parentRel = relativePath.substringBeforeLast('/', "")
            val baseName = src.nameWithoutExtension
            val ext = if (!src.isDirectory && src.extension.isNotEmpty()) ".${src.extension}" else ""
            var index = 1
            var candidateRel: String
            var candidateFile: File
            do {
                val suffix = if (index == 1) "_copy" else "_copy$index"
                val name = "$baseName$suffix$ext"
                candidateRel = if (parentRel.isEmpty()) name else "$parentRel/$name"
                candidateFile = resolveSafe(root, candidateRel)
                index++
            } while (candidateFile.exists())

            if (src.isDirectory) {
                src.copyRecursively(candidateFile, overwrite = false)
            } else {
                src.copyTo(candidateFile, overwrite = false)
            }
            candidateRel
        }
    }

    suspend fun moveItem(
        rootFolderName: String,
        sourceRelativePath: String,
        targetFolderRelativePath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val root = getProjectRoot(rootFolderName)
            val src = resolveSafe(root, sourceRelativePath)
            require(src.exists()) { "Source item not found" }
            val cleanTargetFolder = sanitizeRelativePath(targetFolderRelativePath)
            val destRel = if (cleanTargetFolder.isEmpty()) src.name else "$cleanTargetFolder/${src.name}"
            require(destRel != sourceRelativePath) { "Source and destination are identical" }
            if (src.isDirectory && (destRel == sourceRelativePath || destRel.startsWith("$sourceRelativePath/"))) {
                error("Cannot move a folder into itself")
            }
            val dst = resolveSafe(root, destRel)
            require(!dst.exists()) { "Target already exists" }
            dst.parentFile?.let { if (!it.exists()) it.mkdirs() }
            if (!src.renameTo(dst)) {
                if (src.isDirectory) {
                    src.copyRecursively(dst, overwrite = false)
                    src.deleteRecursively()
                } else {
                    src.copyTo(dst, overwrite = false)
                    src.delete()
                }
            }
            destRel
        }
    }

    suspend fun deleteItem(rootFolderName: String, relativePath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = getProjectRoot(rootFolderName)
                val target = resolveSafe(root, relativePath)
                if (target.exists()) {
                    check(target.deleteRecursively()) { "Failed to delete $relativePath" }
                }
            }
        }

    suspend fun countFiles(rootFolderName: String): Int = withContext(Dispatchers.IO) {
        countFiles(getProjectRoot(rootFolderName))
    }

    private fun countFiles(dir: File): Int {
        if (!dir.exists()) return 0
        var count = 0
        dir.walkTopDown().forEach {
            if (it.isFile && !it.name.endsWith(".mcs_tmp")) count++
        }
        return count
    }

    suspend fun getStorageStats(projectFolders: List<String>): StorageStats = withContext(Dispatchers.IO) {
        var totalFiles = 0
        var totalBytes = 0L
        projectFolders.forEach { folder ->
            val dir = File(projectsBaseDir, folder)
            if (dir.exists()) {
                dir.walkTopDown().forEach { f ->
                    if (f.isFile && !f.name.endsWith(".mcs_tmp")) {
                        totalFiles++
                        totalBytes += f.length()
                    }
                }
            }
        }
        StorageStats(
            projectCount = projectFolders.size,
            totalFiles = totalFiles,
            totalBytes = totalBytes
        )
    }

    suspend fun clearAppCache(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
            true
        }.getOrDefault(false)
    }

    suspend fun exportProjectToZip(rootFolderName: String, outputUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = getProjectRoot(rootFolderName)
                val rootPath = root.canonicalPath
                val resolver = context.contentResolver
                resolver.openOutputStream(outputUri)?.use { os ->
                    ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                        root.walkTopDown()
                            .filter { it.isFile && !it.name.endsWith(".mcs_tmp") }
                            .forEach { file ->
                                val rel = file.canonicalPath.removePrefix(rootPath).trimStart(File.separatorChar)
                                    .replace(File.separatorChar, '/')
                                if (rel.isNotEmpty()) {
                                    zos.putNextEntry(ZipEntry(rel))
                                    FileInputStream(file).use { fis ->
                                        fis.copyTo(zos)
                                    }
                                    zos.closeEntry()
                                }
                            }
                    }
                } ?: error("Cannot open output stream for ZIP export")
            }
        }

    suspend fun importZipToProject(
        inputUri: Uri,
        destFolderName: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val destRoot = getProjectRoot(destFolderName)
            val destCanonical = destRoot.canonicalPath
            val resolver = context.contentResolver
            var importedCount = 0
            resolver.openInputStream(inputUri)?.use { rawIn ->
                ZipInputStream(BufferedInputStream(rawIn)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name.replace('\\', '/').trimStart('/')
                        if (name.isNotEmpty() && !name.contains("__MACOSX")) {
                            val outFile = File(destRoot, name)
                            val outCanonical = outFile.canonicalPath
                            val prefix = if (destCanonical.endsWith(File.separator)) destCanonical else "$destCanonical${File.separator}"
                            if (outCanonical.startsWith(prefix)) {
                                if (entry.isDirectory) {
                                    outFile.mkdirs()
                                } else {
                                    outFile.parentFile?.mkdirs()
                                    FileOutputStream(outFile).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                    importedCount++
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: error("Cannot open ZIP archive")
            importedCount
        }
    }

    private fun sanitizeRelativePath(path: String): String {
        return path.replace('\\', '/')
            .split('/')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "." && it != ".." }
            .joinToString("/")
    }

    private fun resolveSafe(root: File, relativePath: String): File {
        val clean = relativePath.replace('\\', '/').trimStart('/')
        val rootCanonical = root.canonicalFile
        val target = File(rootCanonical, clean).canonicalFile
        val rootPrefix = if (rootCanonical.path.endsWith(File.separator)) {
            rootCanonical.path
        } else {
            rootCanonical.path + File.separator
        }
        require(target.path.startsWith(rootPrefix)) {
            "Path traversal blocked: $relativePath"
        }
        return target
    }
}
