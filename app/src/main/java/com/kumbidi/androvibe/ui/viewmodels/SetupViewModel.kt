package com.kumbidi.androvibe.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.kumbidi.androvibe.ai.AiSetupManager

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val setupManager = AiSetupManager(application)

    var provider by mutableStateOf("gemini")
    var geminiApiKey by mutableStateOf("")
    var geminiModel by mutableStateOf("gemini-2.0-flash")
    var ollamaUrl by mutableStateOf("http://192.168.1.100:11434")
    var ollamaModel by mutableStateOf("")
    var ollamaModels by mutableStateOf<List<String>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun isSetupComplete(): Boolean = setupManager.hasCompletedSetup()

    fun saveGeminiConfig() {
        if (geminiApiKey.isBlank()) {
            errorMessage = "API key cannot be empty"
            return
        }
        setupManager.saveGeminiConfig(geminiApiKey.trim(), geminiModel)
        errorMessage = null
    }

    fun saveOllamaConfig() {
        if (ollamaUrl.isBlank()) {
            errorMessage = "Server URL cannot be empty"
            return
        }
        if (ollamaModel.isBlank()) {
            errorMessage = "Please select a model"
            return
        }
        setupManager.saveOllamaConfig(ollamaUrl.trim(), ollamaModel)
        errorMessage = null
    }

    fun fetchOllamaModels() {
        isLoading = true
        val orchestrator = com.kumbidi.androvibe.ai.AiOrchestrator(setupManager)
        orchestrator.fetchOllamaModels(ollamaUrl.trim()) { models ->
            ollamaModels = models
            isLoading = false
            if (models.isEmpty()) {
                errorMessage = "No models found or cannot reach server"
            }
        }
    }
}
