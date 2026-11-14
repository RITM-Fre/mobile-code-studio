package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.CustomStarterFile
import com.example.core.ProjectTemplateId
import com.example.data.db.ProjectEntity
import com.example.data.db.RecentFileEntity
import com.example.data.repository.IdeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProjectSortOrder {
    UPDATED_DESC,
    NAME_ASC
}

data class DashboardUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val filteredProjects: List<ProjectEntity> = emptyList(),
    val recentFiles: List<RecentFileEntity> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: ProjectSortOrder = ProjectSortOrder.UPDATED_DESC,
    val isBusy: Boolean = false
)

sealed interface DashboardEvent {
    data class OpenProject(val projectId: String, val initialFile: String? = null) : DashboardEvent
    data class Message(val text: String, val isError: Boolean = false) : DashboardEvent
}

class DashboardViewModel(
    private val repository: IdeRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(ProjectSortOrder.UPDATED_DESC)
    private val isBusy = MutableStateFlow(false)

    private val _events = MutableSharedFlow<DashboardEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.projects,
        repository.recentFiles,
        searchQuery,
        sortOrder,
        isBusy
    ) { projects, recents, query, sort, busy ->
        val filtered = projects
            .filter { p ->
                query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.description.contains(query, ignoreCase = true)
            }
            .let { list ->
                when (sort) {
                    ProjectSortOrder.UPDATED_DESC -> list.sortedByDescending { it.updatedAt }
                    ProjectSortOrder.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                }
            }
        DashboardUiState(
            projects = projects,
            filteredProjects = filtered,
            recentFiles = recents,
            searchQuery = query,
            sortOrder = sort,
            isBusy = busy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSortOrder(order: ProjectSortOrder) {
        sortOrder.value = order
    }

    fun createProject(
        name: String,
        description: String,
        templateId: ProjectTemplateId,
        customSelections: Set<CustomStarterFile>,
        openImmediately: Boolean = true
    ) {
        viewModelScope.launch {
            isBusy.value = true
            val res = repository.createProject(name, description, templateId, customSelections)
            isBusy.value = false
            res.onSuccess { created ->
                if (openImmediately) {
                    _events.emit(DashboardEvent.OpenProject(created.id))
                }
            }.onFailure { err ->
                _events.emit(DashboardEvent.Message(err.localizedMessage ?: "Failed", isError = true))
            }
        }
    }

    fun renameProject(projectId: String, newName: String) {
        viewModelScope.launch {
            repository.renameProject(projectId, newName)
                .onFailure { err ->
                    _events.emit(DashboardEvent.Message(err.localizedMessage ?: "Rename failed", isError = true))
                }
        }
    }

    fun duplicateProject(projectId: String, copySuffix: String) {
        viewModelScope.launch {
            isBusy.value = true
            repository.duplicateProject(projectId, copySuffix)
                .onFailure { err ->
                    _events.emit(DashboardEvent.Message(err.localizedMessage ?: "Duplicate failed", isError = true))
                }
            isBusy.value = false
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
                .onFailure { err ->
                    _events.emit(DashboardEvent.Message(err.localizedMessage ?: "Delete failed", isError = true))
                }
        }
    }

    fun exportProjectZip(project: ProjectEntity, uri: Uri, successMsg: String, failMsg: String) {
        viewModelScope.launch {
            isBusy.value = true
            repository.exportProjectZip(project, uri)
                .onSuccess { _events.emit(DashboardEvent.Message(successMsg)) }
                .onFailure { _events.emit(DashboardEvent.Message(failMsg, isError = true)) }
            isBusy.value = false
        }
    }

    fun importProjectZip(uri: Uri, suggestedName: String, successMsg: String, failMsg: String) {
        viewModelScope.launch {
            isBusy.value = true
            repository.importProjectZip(uri, suggestedName)
                .onSuccess { imported ->
                    _events.emit(DashboardEvent.Message(successMsg))
                    _events.emit(DashboardEvent.OpenProject(imported.id))
                }
                .onFailure {
                    _events.emit(DashboardEvent.Message(failMsg, isError = true))
                }
            isBusy.value = false
        }
    }

    class Factory(private val repository: IdeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(repository) as T
    }
}
