package com.kumbidi.androvibe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kumbidi.androvibe.data.local.FileNode
import com.kumbidi.androvibe.ui.theme.*
import com.kumbidi.androvibe.ui.viewmodels.EditorViewModel
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectName: String,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = viewModel()
) {
    LaunchedEffect(projectName) {
        viewModel.loadProject(projectName)
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Mantle,
                modifier = Modifier.width(280.dp)
            ) {
                FileDrawerContent(viewModel) {
                    scope.launch { drawerState.close() }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(projectName, style = MaterialTheme.typography.titleMedium) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Mantle,
                        titleContentColor = Text
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, "Files", tint = Blue)
                        }
                    },
                    actions = {
                        if (viewModel.activeFile?.isModified == true) {
                            IconButton(onClick = { viewModel.saveActiveFile() }) {
                                Icon(Icons.Default.Save, "Save", tint = Green)
                            }
                        }
                        IconButton(onClick = { viewModel.showNewFileDialog = true }) {
                            Icon(Icons.Default.NoteAdd, "New File", tint = Subtext0)
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // Tab bar for open files
                if (viewModel.openFiles.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface0)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        viewModel.openFiles.forEachIndexed { index, file ->
                            val isActive = index == viewModel.activeFileIndex
                            Surface(
                                onClick = { viewModel.activeFileIndex = index },
                                color = if (isActive) Base else Color.Transparent,
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (file.isModified) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Peach, RoundedCornerShape(50))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        file.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isActive) Text else Overlay0
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { viewModel.closeFile(index) },
                                        tint = Overlay0
                                    )
                                }
                            }
                        }
                    }
                }

                // Editor content
                val activeFile = viewModel.activeFile
                if (activeFile != null) {
                    AndroidView(
                        factory = { context ->
                            CodeEditor(context).apply {
                                setText(activeFile.content)
                                isWordwrap = false
                                typefaceText = android.graphics.Typeface.MONOSPACE
                                setBackgroundColor(android.graphics.Color.parseColor("#1E1E2E"))
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { editor ->
                            val currentText = editor.text.toString()
                            if (currentText != activeFile.content && !activeFile.isModified) {
                                editor.setText(activeFile.content)
                            }
                            val editorText = editor.text.toString()
                            if (editorText != activeFile.content) {
                                viewModel.updateFileContent(editorText)
                            }
                        }
                    )
                } else {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Base),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Surface2
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Open a file from the menu",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Overlay0
                        )
                        Text(
                            "Swipe right or tap the menu icon",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Surface2
                        )
                    }
                }
            }
        }
    }

    // New file dialog
    if (viewModel.showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showNewFileDialog = false },
            title = { Text("Create New") },
            text = {
                Column {
                    OutlinedTextField(
                        value = viewModel.newFileName,
                        onValueChange = { viewModel.newFileName = it },
                        label = { Text("Name") },
                        placeholder = { Text("e.g. src/main.py") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = viewModel.isNewFileDir,
                            onCheckedChange = { viewModel.isNewFileDir = it }
                        )
                        Text("Create as folder", color = Text)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.createNewFile() }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showNewFileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FileDrawerContent(
    viewModel: EditorViewModel,
    onFileOpened: () -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface0)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FolderOpen, null, tint = Blue)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "File Explorer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Text
            )
        }

        HorizontalDivider(color = Surface0)

        // File tree
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(viewModel.fileTree) { node ->
                FileTreeItem(
                    node = node,
                    onClick = {
                        viewModel.openFile(node)
                        onFileOpened()
                    },
                    onDelete = { viewModel.deleteFileNode(node) }
                )
            }
        }
    }
}

@Composable
private fun FileTreeItem(
    node: FileNode,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!node.isDirectory) onClick() }
            .padding(
                start = (16 + node.depth * 16).dp,
                top = 6.dp,
                bottom = 6.dp,
                end = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (node.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (node.isDirectory) Blue else fileIconColor(node.name)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            node.name,
            style = MaterialTheme.typography.labelMedium,
            color = Text,
            modifier = Modifier.weight(1f)
        )

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(14.dp), tint = Overlay0)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Delete", color = Red) },
                    onClick = { onDelete(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Red) }
                )
            }
        }
    }
}

private fun fileIconColor(name: String): Color {
    return when {
        name.endsWith(".kt") || name.endsWith(".java") -> Mauve
        name.endsWith(".py") -> Yellow
        name.endsWith(".js") || name.endsWith(".ts") -> Peach
        name.endsWith(".html") || name.endsWith(".xml") -> Red
        name.endsWith(".css") -> Blue
        name.endsWith(".json") || name.endsWith(".yaml") -> Green
        name.endsWith(".md") -> Sapphire
        else -> Overlay0
    }
}
