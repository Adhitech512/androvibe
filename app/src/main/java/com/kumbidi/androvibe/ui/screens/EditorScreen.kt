package com.kumbidi.androvibe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage

@Composable
fun EditorScreen() {
    var editorCode by remember { mutableStateOf("// Welcome to AndroVibe\n// AI Powered Code Sandbox\n\nfun main() {\n    println(\"Hello World\")\n}") }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Workspace / main.kt",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
        }

        AndroidView(
            factory = { context ->
                val editor = CodeEditor(context)
                // Basic Setup for Sora Editor
                editor.setText(editorCode)
                editor.isWordwrap = false
                editor.typefaceText = android.graphics.Typeface.MONOSPACE
                
                // Color Scheme integration
                // Utilizing default Sora Editor dark theme behavior
                
                editor
            },
            modifier = Modifier.fillMaxSize(),
            update = { editor ->
                // Allows Compose state to update the editor text safely if changed externally
                if (editor.text.toString() != editorCode) {
                    editor.setText(editorCode)
                }
            }
        )
    }
}
