package com.kumbidi.androvibe.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kumbidi.androvibe.terminal.TerminalManager
import kotlinx.coroutines.launch

data class TerminalLine(
    val text: String,
    val isInput: Boolean = false
)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val terminalManager = TerminalManager(application)

    var lines by mutableStateOf<List<TerminalLine>>(listOf(TerminalLine("AndroVibe Terminal v1.0")))
    var currentInput by mutableStateOf("")
    var isBootstrapped by mutableStateOf(terminalManager.isBootstrapped())
    var isBootstrapping by mutableStateOf(false)
    var isExecuting by mutableStateOf(false)
    var projectDir by mutableStateOf<java.io.File?>(null)

    fun setWorkingDir(dir: java.io.File) {
        projectDir = dir
        addLine("Working directory: ${dir.absolutePath}")
    }

    fun executeCommand() {
        val cmd = currentInput.trim()
        if (cmd.isBlank()) return

        addLine("$ $cmd", isInput = true)
        currentInput = ""
        isExecuting = true

        viewModelScope.launch {
            val result = if (cmd == "bootstrap") {
                bootstrapProot()
                return@launch
            } else if (isBootstrapped && cmd.startsWith("proot ")) {
                terminalManager.executeInProot(cmd.removePrefix("proot "), projectDir)
            } else {
                terminalManager.executeCommand(cmd, projectDir)
            }

            if (result.output.isNotBlank()) {
                addLine(result.output)
            }
            if (result.exitCode != 0) {
                addLine("[exit code: ${result.exitCode}]")
            }
            isExecuting = false
        }
    }

    private suspend fun bootstrapProot() {
        isBootstrapping = true
        addLine("Starting PRoot bootstrap...")
        val result = terminalManager.bootstrap { status ->
            viewModelScope.launch {
                addLine(status)
            }
        }
        result.onSuccess {
            isBootstrapped = true
            addLine("PRoot environment ready! Use 'proot <command>' prefix.")
        }.onFailure {
            addLine("Bootstrap failed: ${it.message}")
        }
        isBootstrapping = false
        isExecuting = false
    }

    private fun addLine(text: String, isInput: Boolean = false) {
        lines = lines + TerminalLine(text, isInput)
    }

    fun clearTerminal() {
        lines = listOf(TerminalLine("Terminal cleared."))
    }
}
