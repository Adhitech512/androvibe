package com.kumbidi.androvibe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kumbidi.androvibe.ui.theme.*
import com.kumbidi.androvibe.ui.viewmodels.ProjectViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: ProjectViewModel = viewModel(),
    onProjectSelected: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AndroVibe", fontWeight = FontWeight.Bold)
                        Text(
                            "Projects",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Subtext0
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Mantle,
                    titleContentColor = Text
                ),
                actions = {
                    IconButton(onClick = { viewModel.refreshProjects() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Blue)
                    }
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FloatingActionButton(
                    onClick = { viewModel.showCloneDialog = true },
                    containerColor = Mauve,
                    contentColor = Crust,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, "Clone")
                }
                FloatingActionButton(
                    onClick = { viewModel.showNewProjectDialog = true },
                    containerColor = Blue,
                    contentColor = Crust,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, "New Project")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Error banner
            viewModel.error?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Red.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(err, color = Red, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (viewModel.projects.isEmpty() && !viewModel.isLoading) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Surface2
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No projects yet", style = MaterialTheme.typography.titleLarge, color = Subtext0)
                    Text(
                        "Create a new project or clone from Git",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Overlay0
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.projects) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { onProjectSelected(project.name) },
                            onDelete = { viewModel.deleteProject(project.name) }
                        )
                    }
                }
            }

            if (viewModel.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Blue
                )
            }
        }
    }

    // New Project Dialog
    if (viewModel.showNewProjectDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showNewProjectDialog = false },
            title = { Text("New Project") },
            text = {
                OutlinedTextField(
                    value = viewModel.newProjectName,
                    onValueChange = { viewModel.newProjectName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.createProject() }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showNewProjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clone Dialog
    if (viewModel.showCloneDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showCloneDialog = false },
            title = { Text("Clone Repository") },
            text = {
                Column {
                    OutlinedTextField(
                        value = viewModel.cloneUrl,
                        onValueChange = { viewModel.cloneUrl = it },
                        label = { Text("Git URL") },
                        placeholder = { Text("https://github.com/user/repo.git") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.cloneProjectName,
                        onValueChange = { viewModel.cloneProjectName = it },
                        label = { Text("Project Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.cloneRepository() },
                    enabled = !viewModel.isLoading
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Clone")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showCloneDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectCard(
    project: File,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val fileCount = project.listFiles()?.size ?: 0

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface0),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = Blue,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Text
                )
                Text(
                    "$fileCount items · ${dateFormat.format(Date(project.lastModified()))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Overlay0
                )
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, "Delete", tint = Red.copy(alpha = 0.7f))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Project?") },
            text = { Text("\"${project.name}\" and all its files will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Delete", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
