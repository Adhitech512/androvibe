package com.kumbidi.androvibe.ai

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiOrchestrator(private val config: AiSetupManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .build()

    private val gson = Gson()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    // ── Streaming chat ───────────────────────────────────────────
    fun streamChat(prompt: String): Flow<String> = callbackFlow {
        val provider = config.getActiveProvider()

        if (provider == "gemini") {
            streamGemini(prompt)
        } else {
            streamOllama(prompt)
        }

        awaitClose { }
    }

    // ── Gemini via SSE ───────────────────────────────────────────
    private fun kotlinx.coroutines.channels.ProducerScope<String>.streamGemini(prompt: String) {
        val apiKey = config.getGeminiApiKey() ?: run {
            close(Exception("Gemini API key not configured"))
            return
        }
        val model = config.getGeminiModel()

        val body = gson.toJson(
            mapOf("contents" to listOf(mapOf("parts" to listOf(mapOf("text" to prompt)))))
        ).toRequestBody(jsonType)

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey")
            .post(body)
            .build()

        val factory = EventSources.createFactory(client)
        factory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val json = JsonParser.parseString(data).asJsonObject
                    val candidates = json.getAsJsonArray("candidates")
                    if (candidates != null && candidates.size() > 0) {
                        val parts = candidates[0].asJsonObject
                            .getAsJsonObject("content")
                            ?.getAsJsonArray("parts")
                        if (parts != null && parts.size() > 0) {
                            val text = parts[0].asJsonObject.get("text")?.asString ?: ""
                            trySend(text)
                        }
                    }
                } catch (e: Exception) {
                    // skip malformed chunks
                }
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t ?: Exception("Gemini request failed: ${response?.code}"))
            }
        })
    }

    // ── Ollama via streaming JSON lines ──────────────────────────
    private fun kotlinx.coroutines.channels.ProducerScope<String>.streamOllama(prompt: String) {
        val url = config.getOllamaServerUrl() ?: run {
            close(Exception("Ollama server URL not configured"))
            return
        }
        val model = config.getOllamaModel() ?: "llama3"

        val body = gson.toJson(
            mapOf("model" to model, "prompt" to prompt, "stream" to true)
        ).toRequestBody(jsonType)

        val request = Request.Builder()
            .url("$url/api/generate")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    response.body?.source()?.let { source ->
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (line.isNotBlank()) {
                                val chunk = JsonParser.parseString(line).asJsonObject
                                val text = chunk.get("response")?.asString ?: ""
                                if (text.isNotEmpty()) trySend(text)
                                if (chunk.get("done")?.asBoolean == true) break
                            }
                        }
                    }
                } catch (e: Exception) {
                    // stream ended
                } finally {
                    close()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }
        })
    }

    // ── Fetch Ollama models list ─────────────────────────────────
    fun fetchOllamaModels(serverUrl: String, callback: (List<String>) -> Unit) {
        val request = Request.Builder()
            .url("$serverUrl/api/tags")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: "{}"
                    val json = JsonParser.parseString(body).asJsonObject
                    val models = json.getAsJsonArray("models")
                    val names = mutableListOf<String>()
                    if (models != null) {
                        for (m in models) {
                            val name = m.asJsonObject.get("name")?.asString
                            if (name != null) names.add(name)
                        }
                    }
                    callback(names)
                } catch (e: Exception) {
                    callback(emptyList())
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(emptyList())
            }
        })
    }
}
