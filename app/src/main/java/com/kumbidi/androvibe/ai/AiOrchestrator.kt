package com.kumbidi.androvibe.ai

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

class AiOrchestrator(private val configManager: AiSetupManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES) // For long AI generation
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Sends a message to the active AI (Gemini or Ollama) and streams the response text back.
     */
    fun streamChatResponse(prompt: String): Flow<String> = callbackFlow {
        val useGemini = configManager.getGeminiApiKey() != null
        
        if (useGemini) {
            val apiKey = configManager.getGeminiApiKey() ?: throw Exception("Gemini API Key missing")
            val model = configManager.getGeminiModel()
            
            // Build Gemini Request
            val requestBody = buildGeminiRequestBody(prompt)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey")
                .post(requestBody)
                .build()

            val factory = EventSources.createFactory(client)
            factory.newEventSource(request, object : EventSourceListener() {
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    try {
                        // Parse Gemini SSE chunk
                        // simplified: assumes structured json with "text" field somewhere
                        if (data.isNotBlank()) {
                            trySend(parseGeminiChunk(data))
                        }
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    close()
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    close(t ?: Exception("Network failure: ${response?.code}"))
                }
            })
        } else {
            val url = configManager.getOllamaServerUrl() ?: throw Exception("Ollama URL missing")
            val model = configManager.getOllamaModel() ?: "llama3"

            // Construct Ollama request
            val jsonMap = mapOf(
                "model" to model,
                "prompt" to prompt,
                "stream" to true
            )
            val requestBody = gson.toJson(jsonMap).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$url/api/generate")
                .post(requestBody)
                .build()

            // Ollama streams JSON by default per line, not standard SSE, but similar processing
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Unexpected code $response")

                    response.body?.source()?.let { source ->
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line()
                            if (line != null) {
                                val chunk = gson.fromJson(line, Map::class.java)
                                val text = chunk["response"] as? String ?: ""
                                trySend(text)
                            }
                        }
                    }
                    close()
                }
            }
        }
        awaitClose { }
    }

    private fun buildGeminiRequestBody(prompt: String): RequestBody {
        val map = mapOf(
            "contents" to listOf(
                mapOf("parts" to listOf(mapOf("text" to prompt)))
            )
        )
        return gson.toJson(map).toRequestBody(jsonMediaType)
    }

    private fun parseGeminiChunk(data: String): String {
        // Pseudo-parser for Gemini chunk. Full implementation requires mapping the gemini schema
        // using Gson. We extract text node string here.
        if(data.contains("\"text\": \"")) {
            val start = data.indexOf("\"text\": \"") + 9
            val end = data.indexOf("\"", start)
            if(end > start) {
                return data.substring(start, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
            }
        }
        return ""
    }
}
