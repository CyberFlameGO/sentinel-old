package com.fredboat.sentinel.entities

data class AudioQueueRequest(
        val type: AudioQueueRequestEnum,
        val guild: Long,
        val channel: Long? = null // Only used with QUEUE_CONNECT
)

enum class AudioQueueRequestEnum {
    REMOVE,
    QUEUE_DISCONNECT,
    QUEUE_CONNECT
}

data class VoiceServerUpdate(
        val sessionId: String,
        val raw: String // The raw JSON from Discord
)