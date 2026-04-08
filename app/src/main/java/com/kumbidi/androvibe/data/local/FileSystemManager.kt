package com.kumbidi.androvibe.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class FileNode(
    val name: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val children: List<FileNode> = emptyList(),
    val depth: Int = 0
)

class FileSystemManager(private val context: Context) {

    val projectsRoot: File by lazy {
        val dir = context.getExternalFilesDir("Projects")
            ?: context.getDir("Projects", Context.MODE_PRIVATE)
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    // ── Project operations ───────────────────────────────────────
    fun listProjects(): List<File> {
        return projectsRoot.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name } ?: emptyList()
    }

    suspend fun createProject(name: String): Result<File> = withContext(Dispatchers.IO) {
        val sanitized = name.replace(Regex("[^a-zA-Z0-9_\\-.]"), "_")
        val dir = File(projectsRoot, sanitized)
        if (dir.exists()) return@withContext Result.failure(Exception("Project '$sanitized' already exists"))
        if (dir.mkdirs()) Result.success(dir) else Result.failure(Exception("Failed to create directory"))
    }

    suspend fun deleteProject(name: String): Boolean = withContext(Dispatchers.IO) {
        val dir = File(projectsRoot, name)
        if (!dir.exists()) return@withContext false
        dir.deleteRecursively()
    }

    // ── File tree ────────────────────────────────────────────────
    fun getFileTree(projectName: String): List<FileNode> {
        val root = File(projectsRoot, projectName)
        if (!root.exists()) return emptyList()
        return flattenTree(root, 0)
    }

    private fun flattenTree(dir: File, depth: Int): List<FileNode> {
        val result = mutableListOf<FileNode>()
        val children = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: return result
        for (child in children) {
            if (child.name.startsWith(".")) continue // hide dotfiles
            result.add(FileNode(child.name, child.absolutePath, child.isDirectory, depth = depth))
            if (child.isDirectory) {
                result.addAll(flattenTree(child, depth + 1))
            }
        }
        return result
    }

    // ── File CRUD (sandboxed) ────────────────────────────────────
    fun readFile(projectName: String, relativePath: String): Result<String> {
        val projectDir = File(projectsRoot, projectName)
        val target = File(projectDir, relativePath).canonicalFile
        if (!target.absolutePath.startsWith(projectDir.canonicalPath)) {
            return Result.failure(SecurityException("Path escapes sandbox"))
        }
        if (!target.exists() || !target.isFile) {
            return Result.failure(Exception("File not found: $relativePath"))
        }
        return Result.success(target.readText())
    }

    fun writeFile(projectName: String, relativePath: String, content: String): Result<Unit> {
        val projectDir = File(projectsRoot, projectName)
        val target = File(projectDir, relativePath).canonicalFile
        if (!target.absolutePath.startsWith(projectDir.canonicalPath)) {
            return Result.failure(SecurityException("Path escapes sandbox"))
        }
        target.parentFile?.mkdirs()
        target.writeText(content)
        return Result.success(Unit)
    }

    fun createFile(projectName: String, relativePath: String): Result<Unit> {
        val projectDir = File(projectsRoot, projectName)
        val target = File(projectDir, relativePath).canonicalFile
        if (!target.absolutePath.startsWith(projectDir.canonicalPath)) {
            return Result.failure(SecurityException("Path escapes sandbox"))
        }
        target.parentFile?.mkdirs()
        target.createNewFile()
        return Result.success(Unit)
    }

    fun createFolder(projectName: String, relativePath: String): Result<Unit> {
        val projectDir = File(projectsRoot, projectName)
        val target = File(projectDir, relativePath).canonicalFile
        if (!target.absolutePath.startsWith(projectDir.canonicalPath)) {
            return Result.failure(SecurityException("Path escapes sandbox"))
        }
        target.mkdirs()
        return Result.success(Unit)
    }

    fun deleteFile(projectName: String, relativePath: String): Result<Unit> {
        val projectDir = File(projectsRoot, projectName)
        val target = File(projectDir, relativePath).canonicalFile
        if (!target.absolutePath.startsWith(projectDir.canonicalPath)) {
            return Result.failure(SecurityException("Path escapes sandbox"))
        }
        if (target.isDirectory) target.deleteRecursively() else target.delete()
        return Result.success(Unit)
    }

    fun renameFile(projectName: String, oldPath: String, newName: String): Result<Unit> {
        val projectDir = File(projectsRoot, projectName)
        val source = File(projectDir, oldPath).canonicalFile
        if (!source.absolutePath.startsWith(projectDir.canonicalPath)) {
            return Result.failure(SecurityException("Path escapes sandbox"))
        }
        val dest = File(source.parentFile, newName)
        if (source.renameTo(dest)) return Result.success(Unit)
        return Result.failure(Exception("Rename failed"))
    }

    // ── Export project to a target directory ─────────────────────
    suspend fun exportProject(projectName: String, targetDir: File): Result<Unit> = withContext(Dispatchers.IO) {
        val source = File(projectsRoot, projectName)
        if (!source.exists()) return@withContext Result.failure(Exception("Project not found"))
        try {
            source.copyRecursively(File(targetDir, projectName), overwrite = true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Import folder into project ──────────────────────────────
    suspend fun importFolder(sourceDir: File, projectName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val dest = File(projectsRoot, projectName)
        try {
            sourceDir.copyRecursively(dest, overwrite = true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
