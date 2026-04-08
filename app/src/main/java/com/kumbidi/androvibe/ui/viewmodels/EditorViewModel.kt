package com.kumbidi.androvibe.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.kumbidi.androvibe.data.local.FileNode
import com.kumbidi.androvibe.data.local.FileSystemManager

data class OpenFile(
    val relativePath: String,
    val name: String,
    val content: String,
    val isModified: Boolean = false
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val fsManager = FileSystemManager(application)

    var projectName by mutableStateOf("")
    var fileTree by mutableStateOf<List<FileNode>>(emptyList())
    var openFiles by mutableStateOf<List<OpenFile>>(emptyList())
    var activeFileIndex by mutableStateOf(-1)
    var showFileDrawer by mutableStateOf(false)
    var showNewFileDialog by mutableStateOf(false)
    var newFileName by mutableStateOf("")
    var isNewFileDir by mutableStateOf(false)

    val activeFile: OpenFile?
        get() = if (activeFileIndex in openFiles.indices) openFiles[activeFileIndex] else null

    fun loadProject(name: String) {
        projectName = name
        refreshFileTree()
    }

    fun refreshFileTree() {
        fileTree = fsManager.getFileTree(projectName)
    }

    fun openFile(node: FileNode) {
        if (node.isDirectory) return

        // Check if already open
        val existingIndex = openFiles.indexOfFirst { it.relativePath == node.absolutePath }
        if (existingIndex != -1) {
            activeFileIndex = existingIndex
            return
        }

        // Calculate relative path from project root
        val projectDir = java.io.File(fsManager.projectsRoot, projectName)
        val relativePath = java.io.File(node.absolutePath).relativeTo(projectDir).path

        val result = fsManager.readFile(projectName, relativePath)
        result.onSuccess { content ->
            val newList = openFiles.toMutableList()
            newList.add(OpenFile(node.absolutePath, node.name, content))
            openFiles = newList
            activeFileIndex = newList.size - 1
        }
    }

    fun closeFile(index: Int) {
        if (index !in openFiles.indices) return
        val newList = openFiles.toMutableList()
        newList.removeAt(index)
        openFiles = newList
        activeFileIndex = when {
            newList.isEmpty() -> -1
            index >= newList.size -> newList.size - 1
            else -> index
        }
    }

    fun updateFileContent(content: String) {
        if (activeFileIndex !in openFiles.indices) return
        val newList = openFiles.toMutableList()
        newList[activeFileIndex] = newList[activeFileIndex].copy(content = content, isModified = true)
        openFiles = newList
    }

    fun saveActiveFile() {
        val file = activeFile ?: return
        val projectDir = java.io.File(fsManager.projectsRoot, projectName)
        val relativePath = java.io.File(file.relativePath).relativeTo(projectDir).path
        fsManager.writeFile(projectName, relativePath, file.content)
        val newList = openFiles.toMutableList()
        newList[activeFileIndex] = newList[activeFileIndex].copy(isModified = false)
        openFiles = newList
    }

    fun createNewFile() {
        if (newFileName.isBlank()) return
        if (isNewFileDir) {
            fsManager.createFolder(projectName, newFileName.trim())
        } else {
            fsManager.createFile(projectName, newFileName.trim())
        }
        newFileName = ""
        showNewFileDialog = false
        refreshFileTree()
    }

    fun deleteFileNode(node: FileNode) {
        val projectDir = java.io.File(fsManager.projectsRoot, projectName)
        val relativePath = java.io.File(node.absolutePath).relativeTo(projectDir).path
        fsManager.deleteFile(projectName, relativePath)
        // Close if open
        val idx = openFiles.indexOfFirst { it.relativePath == node.absolutePath }
        if (idx != -1) closeFile(idx)
        refreshFileTree()
    }
}
