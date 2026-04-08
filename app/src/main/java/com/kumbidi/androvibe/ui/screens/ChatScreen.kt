package com.kumbidi.androvibe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kumbidi.androvibe.ui.theme.*
import com.kumbidi.androvibe.ui.viewmodels.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel()
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom
    LaunchedEffect(viewModel.messages.size, viewModel.messages.lastOrNull()?.content) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Base)
    ) {
        // Header
        Surface(
            color = Mantle,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = Mauve, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AI Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Text
                    )
                    Text(
                        viewModel.getProviderInfo(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Overlay0
                    )
                }
                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(Icons.Default.DeleteSweep, "Clear", tint = Overlay0)
                }
            }
        }

        // Message list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (viewModel.messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Surface2
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Start a conversation",
                            style = MaterialTheme.typography.titleLarge,
                            color = Subtext0
                        )
                        Text(
                            "Ask me to write code, explain concepts,\nor help debug your project.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Overlay0
                        )
                    }
                }
            }

            items(viewModel.messages) { message ->
                MessageBubble(message)
            }
        }

        // Error banner
        viewModel.error?.let { err ->
            Surface(
                color = Red.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    err,
                    color = Red,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Input bar
        Surface(
            color = Mantle,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = viewModel.currentInput,
                    onValueChange = { viewModel.currentInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message AI...", color = Overlay0) },
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Surface0,
                        unfocusedContainerColor = Surface0,
                        focusedBorderColor = Blue,
                        unfocusedBorderColor = Surface1,
                        focusedTextColor = Text,
                        unfocusedTextColor = Text,
                        cursorColor = Blue
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (viewModel.isStreaming) {
                    IconButton(
                        onClick = { viewModel.stopStreaming() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Red)
                    ) {
                        Icon(Icons.Default.Stop, "Stop", tint = Crust)
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.sendMessage() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Blue)
                    ) {
                        Icon(Icons.Default.Send, "Send", tint = Crust)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: com.kumbidi.androvibe.ai.ChatMessage) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Role label
        Text(
            if (isUser) "You" else "AI",
            style = MaterialTheme.typography.labelSmall,
            color = if (isUser) Blue else Mauve,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
        )

        Surface(
            color = if (isUser) Blue.copy(alpha = 0.15f) else Surface0,
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content.ifBlank { if (message.isStreaming) "..." else "" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Text
                )
                if (message.isStreaming) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = Mauve,
                        trackColor = Surface1
                    )
                }
            }
        }
    }
}
