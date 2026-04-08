package com.kumbidi.androvibe.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kumbidi.androvibe.ui.theme.*
import com.kumbidi.androvibe.ui.viewmodels.SetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    viewModel: SetupViewModel = viewModel(),
    onSetupComplete: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Logo / Title
        Text(
            "AndroVibe",
            style = MaterialTheme.typography.headlineLarge,
            color = Blue,
            fontWeight = FontWeight.Bold
        )
        Text(
            "AI-Powered Code Editor",
            style = MaterialTheme.typography.bodyLarge,
            color = Subtext0
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Provider selection
        Text(
            "Choose your AI provider",
            style = MaterialTheme.typography.titleMedium,
            color = Text
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProviderCard(
                title = "Gemini",
                icon = Icons.Default.Cloud,
                selected = viewModel.provider == "gemini",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.provider = "gemini" }
            )
            ProviderCard(
                title = "Ollama",
                icon = Icons.Default.Computer,
                selected = viewModel.provider == "ollama",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.provider = "ollama" }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Config form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Surface0),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (viewModel.provider == "gemini") {
                    GeminiConfigForm(viewModel)
                } else {
                    OllamaConfigForm(viewModel)
                }
            }
        }

        // Error
        AnimatedVisibility(visible = viewModel.errorMessage != null) {
            Text(
                viewModel.errorMessage ?: "",
                color = Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit
        Button(
            onClick = {
                if (viewModel.provider == "gemini") {
                    viewModel.saveGeminiConfig()
                } else {
                    viewModel.saveOllamaConfig()
                }
                if (viewModel.errorMessage == null) {
                    onSetupComplete()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Get Started", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun ProviderCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Blue.copy(alpha = 0.15f) else Surface0
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(
            2.dp, Blue
        ) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (selected) Blue else Overlay0,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) Blue else Text
            )
        }
    }
}

@Composable
private fun GeminiConfigForm(viewModel: SetupViewModel) {
    Text("Gemini API Configuration", style = MaterialTheme.typography.titleMedium, color = Text)
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = viewModel.geminiApiKey,
        onValueChange = { viewModel.geminiApiKey = it },
        label = { Text("API Key") },
        placeholder = { Text("AIza...") },
        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(12.dp))

    // Model selector
    val models = listOf("gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash", "gemini-2.5-pro-preview-03-25")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = viewModel.geminiModel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Model") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        viewModel.geminiModel = model
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OllamaConfigForm(viewModel: SetupViewModel) {
    Text("Ollama Server Configuration", style = MaterialTheme.typography.titleMedium, color = Text)
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = viewModel.ollamaUrl,
        onValueChange = { viewModel.ollamaUrl = it },
        label = { Text("Server URL (ip:port)") },
        placeholder = { Text("http://192.168.1.100:11434") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(12.dp))

    // Fetch models button
    OutlinedButton(
        onClick = { viewModel.fetchOllamaModels() },
        modifier = Modifier.fillMaxWidth(),
        enabled = !viewModel.isLoading
    ) {
        if (viewModel.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.Refresh, contentDescription = null)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("Fetch Available Models")
    }

    if (viewModel.ollamaModels.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = viewModel.ollamaModel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                viewModel.ollamaModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = {
                            viewModel.ollamaModel = model
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
