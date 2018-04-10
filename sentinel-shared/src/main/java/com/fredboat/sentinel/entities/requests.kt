package com.fredboat.sentinel.entities

data class GuildsRequest(
        val shard: Int
)

data class GuildsResponse(
        val guilds: List<Guild>) {

    override fun toString() = "GuildsResponse(guilds.size=${guilds.size})"
}

data class SendMessageRequest(
        val channel: String,
        val content: String
)

data class SendMessageResponse(
        val messageId: String
)

data class SendTypingRequest(
        val channel: String
)