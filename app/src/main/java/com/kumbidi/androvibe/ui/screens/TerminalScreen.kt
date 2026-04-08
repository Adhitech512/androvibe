package com.kumbidi.androvibe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kumbidi.androvibe.ui.theme.*
import com.kumbidi.androvibe.ui.viewmodels.TerminalViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    projectName: String,
    projectDir: File,
    modifier: Modifier = Modifier,
    viewModel: TerminalViewModel = viewModel()
) {
    LaunchedEffect(projectDir) {
        viewModel.setWorkingDir(projectDir)
    }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new lines arrive
    LaunchedEffect(viewModel.lines.size) {
        if (viewModel.lines.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.lines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // Top bar
        Surface(
            color = Color(0xFF1A1A2E),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Terminal, null, tint = Green, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Terminal",
                    style = MaterialTheme.typography.titleMedium,
                    color = Text
                )
                Spacer(modifier = Modifier.weight(1f))

                if (!viewModel.isBootstrapped) {
                    TextButton(
                        onClick = { viewModel.currentInput = "bootstrap"; viewModel.executeCommand() },
                        enabled = !viewModel.isBootstrapping
                    ) {
                        if (viewModel.isBootstrapping) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Peach
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Setup PRoot", color = Peach, style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Text("PRoot ✓", color = Green, style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.clearTerminal() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ClearAll, "Clear", tint = Overlay0, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Terminal output
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(viewModel.lines) { line ->
                Text(
                    text = line.text,
                    color = when {
                        line.isInput -> Green
                        line.text.startsWith("Error") || line.text.startsWith("[exit code:") -> Red
                        line.text.startsWith("Working directory:") -> Blue
                        else -> Color(0xFFB0B0B0)
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        // Loading indicator
        if (viewModel.isExecuting) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Blue,
                trackColor = Color.Transparent
            )
        }

        // Input area
        Surface(
            color = Color(0xFF1A1A2E),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "❯ ",
                    color = Blue,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
                TextField(
                    value = viewModel.currentInput,
                    onValueChange = { viewModel.currentInput = it },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Green
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    placeholder = {
                        Text(
                            "Type command...",
                            color = Surface2,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { viewModel.executeCommand() }
                    )
                )
                IconButton(
                    onClick = { viewModel.executeCommand() },
                    enabled = !viewModel.isExecuting
                ) {
                    Icon(
                        Icons.Default.Send,
                        "Execute",
                        tint = if (viewModel.isExecuting) Surface2 else Blue
                    )
                }
            }
        }
    }
}
