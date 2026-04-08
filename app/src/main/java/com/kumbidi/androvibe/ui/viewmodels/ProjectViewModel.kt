package com.kumbidi.androvibe.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kumbidi.androvibe.data.local.FileSystemManager
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import java.io.File

class ProjectViewModel(application: Application) : AndroidViewModel(application) {

    private val fsManager = FileSystemManager(application)

    var projects by mutableStateOf<List<File>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    // Dialog states
    var showNewProjectDialog by mutableStateOf(false)
    var showCloneDialog by mutableStateOf(false)
    var newProjectName by mutableStateOf("")
    var cloneUrl by mutableStateOf("")
    var cloneProjectName by mutableStateOf("")

    init {
        refreshProjects()
    }

    fun refreshProjects() {
        projects = fsManager.listProjects()
    }

    fun createProject() {
        if (newProjectName.isBlank()) {
            error = "Project name cannot be empty"
            return
        }
        viewModelScope.launch {
            isLoading = true
            val result = fsManager.createProject(newProjectName.trim())
            result.onSuccess {
                showNewProjectDialog = false
                newProjectName = ""
                refreshProjects()
            }.onFailure {
                error = it.message
            }
            isLoading = false
        }
    }

    fun cloneRepository() {
        if (cloneUrl.isBlank() || cloneProjectName.isBlank()) {
            error = "URL and project name are required"
            return
        }
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val targetDir = File(fsManager.projectsRoot, cloneProjectName.trim())
                Git.cloneRepository()
                    .setURI(cloneUrl.trim())
                    .setDirectory(targetDir)
                    .call()
                    .close()
                showCloneDialog = false
                cloneUrl = ""
                cloneProjectName = ""
                refreshProjects()
            } catch (e: Exception) {
                error = "Clone failed: ${e.message}"
            }
            isLoading = false
        }
    }

    fun deleteProject(name: String) {
        viewModelScope.launch {
            fsManager.deleteProject(name)
            refreshProjects()
        }
    }
}
