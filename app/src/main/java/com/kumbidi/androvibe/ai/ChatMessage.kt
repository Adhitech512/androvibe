package com.kumbidi.androvibe.ai

data class ChatMessage(
    val role: String,       // "user", "assistant", or "system"
    val content: String,
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
