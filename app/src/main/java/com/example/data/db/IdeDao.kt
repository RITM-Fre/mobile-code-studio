package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectById(projectId: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: String)

    @Query("SELECT * FROM recent_files ORDER BY openedAt DESC LIMIT 12")
    fun observeRecentFiles(): Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentFile(recentFile: RecentFileEntity)

    @Query("DELETE FROM recent_files WHERE projectId = :projectId")
    suspend fun deleteRecentFilesForProject(projectId: String)

    @Query("DELETE FROM recent_files WHERE projectId = :projectId AND relativePath = :relativePath")
    suspend fun deleteRecentFile(projectId: String, relativePath: String)

    @Query("SELECT * FROM open_tabs WHERE projectId = :projectId ORDER BY sortOrder ASC")
    suspend fun getOpenTabs(projectId: String): List<OpenTabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOpenTabs(tabs: List<OpenTabEntity>)

    @Query("DELETE FROM open_tabs WHERE projectId = :projectId")
    suspend fun clearOpenTabsForProject(projectId: String)

    @Query("SELECT * FROM editor_drafts WHERE projectId = :projectId AND relativePath = :relativePath LIMIT 1")
    suspend fun getDraft(projectId: String, relativePath: String): EditorDraftEntity?

    @Query("SELECT * FROM editor_drafts WHERE projectId = :projectId AND isDirty = 1")
    suspend fun getDirtyDrafts(projectId: String): List<EditorDraftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: EditorDraftEntity)

    @Query("DELETE FROM editor_drafts WHERE projectId = :projectId AND relativePath = :relativePath")
    suspend fun deleteDraft(projectId: String, relativePath: String)

    @Query("DELETE FROM editor_drafts WHERE projectId = :projectId")
    suspend fun deleteDraftsForProject(projectId: String)
}
