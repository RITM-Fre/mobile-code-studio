package com.example.data.fs

data class FileNode(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val extension: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val children: List<FileNode> = emptyList()
)

data class StorageStats(
    val projectCount: Int,
    val totalFiles: Int,
    val totalBytes: Long
)
