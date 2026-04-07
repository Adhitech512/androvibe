package com.kumbidi.androvibe.ai

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AiSetupManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "ai_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun hasCompletedSetup(): Boolean {
        // Return true if at least one API is configured
        return getGeminiApiKey() != null || getOllamaServerUrl() != null
    }

    fun saveGeminiConfig(apiKey: String, modelName: String = "gemini-1.5-pro") {
        sharedPreferences.edit()
            .putString("gemini_api_key", apiKey)
            .putString("gemini_model", modelName)
            .apply()
    }

    fun getGeminiApiKey(): String? = sharedPreferences.getString("gemini_api_key", null)
    fun getGeminiModel(): String = sharedPreferences.getString("gemini_model", "gemini-1.5-pro") ?: "gemini-1.5-pro"

    fun saveOllamaConfig(url: String, modelName: String) {
        sharedPreferences.edit()
            .putString("ollama_url", url)
            .putString("ollama_model", modelName)
            .apply()
    }

    fun getOllamaServerUrl(): String? = sharedPreferences.getString("ollama_url", null)
    fun getOllamaModel(): String? = sharedPreferences.getString("ollama_model", null)
    
    fun clearConfig() {
        sharedPreferences.edit().clear().apply()
    }
}
