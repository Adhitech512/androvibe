package com.kumbidi.androvibe.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kumbidi.androvibe.ai.AiOrchestrator
import com.kumbidi.androvibe.ai.AiSetupManager
import com.kumbidi.androvibe.ai.ChatMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val setupManager = AiSetupManager(application)
    private val orchestrator = AiOrchestrator(setupManager)

    var messages by mutableStateOf<List<ChatMessage>>(emptyList())
    var currentInput by mutableStateOf("")
    var isStreaming by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private var streamJob: Job? = null

    fun sendMessage() {
        val text = currentInput.trim()
        if (text.isBlank() || isStreaming) return

        currentInput = ""
        error = null

        // Add user message
        messages = messages + ChatMessage(role = "user", content = text)

        // Add empty assistant message that will be filled by stream
        messages = messages + ChatMessage(role = "assistant", content = "", isStreaming = true)
        isStreaming = true

        streamJob = viewModelScope.launch {
            val assistantIndex = messages.size - 1
            var accumulated = ""

            orchestrator.streamChat(text)
                .catch { e ->
                    error = e.message
                    // Update the last message to show error
                    val updated = messages.toMutableList()
                    updated[assistantIndex] = updated[assistantIndex].copy(
                        content = accumulated + "\n\n⚠️ Error: ${e.message}",
                        isStreaming = false
                    )
                    messages = updated
                    isStreaming = false
                }
                .onCompletion {
                    // Finalize
                    val updated = messages.toMutableList()
                    if (assistantIndex < updated.size) {
                        updated[assistantIndex] = updated[assistantIndex].copy(isStreaming = false)
                        messages = updated
                    }
                    isStreaming = false
                }
                .collect { chunk ->
                    accumulated += chunk
                    val updated = messages.toMutableList()
                    if (assistantIndex < updated.size) {
                        updated[assistantIndex] = updated[assistantIndex].copy(content = accumulated)
                        messages = updated
                    }
                }
        }
    }

    fun stopStreaming() {
        streamJob?.cancel()
        isStreaming = false
    }

    fun clearChat() {
        stopStreaming()
        messages = emptyList()
    }

    fun getProviderInfo(): String {
        val provider = setupManager.getActiveProvider()
        return if (provider == "gemini") {
            "Gemini · ${setupManager.getGeminiModel()}"
        } else {
            "Ollama · ${setupManager.getOllamaModel() ?: "unknown"}"
        }
    }
}
