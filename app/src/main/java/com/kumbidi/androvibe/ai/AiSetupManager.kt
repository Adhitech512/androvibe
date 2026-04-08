package com.kumbidi.androvibe.ai

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AiSetupManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "ai_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ── Setup State ──────────────────────────────────────────────
    fun hasCompletedSetup(): Boolean {
        return getGeminiApiKey() != null || getOllamaServerUrl() != null
    }

    fun getActiveProvider(): String {
        return prefs.getString("active_provider", "gemini") ?: "gemini"
    }

    fun setActiveProvider(provider: String) {
        prefs.edit().putString("active_provider", provider).apply()
    }

    // ── Gemini ───────────────────────────────────────────────────
    fun saveGeminiConfig(apiKey: String, model: String) {
        prefs.edit()
            .putString("gemini_api_key", apiKey)
            .putString("gemini_model", model)
            .putString("active_provider", "gemini")
            .apply()
    }

    fun getGeminiApiKey(): String? = prefs.getString("gemini_api_key", null)

    fun getGeminiModel(): String =
        prefs.getString("gemini_model", "gemini-2.0-flash") ?: "gemini-2.0-flash"

    // ── Ollama ───────────────────────────────────────────────────
    fun saveOllamaConfig(url: String, model: String) {
        prefs.edit()
            .putString("ollama_url", url)
            .putString("ollama_model", model)
            .putString("active_provider", "ollama")
            .apply()
    }

    fun getOllamaServerUrl(): String? = prefs.getString("ollama_url", null)

    fun getOllamaModel(): String? = prefs.getString("ollama_model", null)

    // ── Reset ────────────────────────────────────────────────────
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
