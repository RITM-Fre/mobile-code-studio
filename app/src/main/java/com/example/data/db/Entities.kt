package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val templateId: String,
    val rootFolderName: String,
    val entryFile: String = "index.html",
    val fileCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val accentIndex: Int = 0
)

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val projectName: String,
    val relativePath: String,
    val fileName: String,
    val openedAt: Long
)

@Entity(tableName = "open_tabs")
data class OpenTabEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val relativePath: String,
    val sortOrder: Int,
    val isActive: Boolean
)

@Entity(tableName = "editor_drafts")
data class EditorDraftEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val relativePath: String,
    val content: String,
    val cursorPosition: Int,
    val isDirty: Boolean,
    val updatedAt: Long
)
