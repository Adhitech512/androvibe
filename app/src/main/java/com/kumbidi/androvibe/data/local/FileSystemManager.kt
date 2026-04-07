package com.kumbidi.androvibe.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import java.io.File
import java.io.IOException

class FileSystemManager(private val context: Context) {

    // Ensures we stay entirely inside the App's isolated sandbox
    private val rootSandboxDir: File by lazy {
        val dir = context.getExternalFilesDir("Projects")
            ?: context.getDir("Projects", Context.MODE_PRIVATE)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    /**
     * Lists all projects in the sandbox.
     */
    fun listProjects(): List<File> {
        return rootSandboxDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
    }

    /**
     * Creates an empty local project directory.
     */
    suspend fun createProject(projectName: String): Boolean = withContext(Dispatchers.IO) {
        val newProj = File(rootSandboxDir, projectName)
        if (newProj.exists()) {
            return@withContext false
        }
        return@withContext newProj.mkdirs()
    }

    /**
     * Clones a repository into the sandbox using JGit.
     */
    suspend fun cloneRepository(repoUrl: String, projectName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val targetDir = File(rootSandboxDir, projectName)
            if (targetDir.exists() && targetDir.listFiles()?.isNotEmpty() == true) {
                return@withContext Result.failure(Exception("Directory already exists and is not empty"))
            }
            
            Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(targetDir)
                .call()
                .close()
                
            Result.success(targetDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads a file. Ensure it does not escape the sandbox (no ../ attacks).
     */
    suspend fun readFile(relativePath: String, projectName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(rootSandboxDir, projectName)
            val targetFile = File(projectDir, relativePath).canonicalFile
            
            // Security check to prevent Sandbox Escaping
            if (!targetFile.absolutePath.startsWith(projectDir.canonicalPath)) {
                return@withContext Result.failure(SecurityException("Attempt to access file outside sandbox"))
            }

            if (!targetFile.exists() || !targetFile.isFile) {
                return@withContext Result.failure(Exception("File does not exist or is a directory"))
            }

            Result.success(targetFile.readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Writes to a file, creating it if it doesn't exist.
     */
    suspend fun writeFile(relativePath: String, projectName: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(rootSandboxDir, projectName)
            val targetFile = File(projectDir, relativePath).canonicalFile

            // Security check
            if (!targetFile.absolutePath.startsWith(projectDir.canonicalPath)) {
                return@withContext Result.failure(SecurityException("Attempt to access file outside sandbox"))
            }

            targetFile.parentFile?.mkdirs()
            targetFile.writeText(content)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}
