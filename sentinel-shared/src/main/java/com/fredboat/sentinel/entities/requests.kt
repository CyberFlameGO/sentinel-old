@file:Suppress("MemberVisibilityCanBePrivate")

package com.fredboat.sentinel.entities

data class GuildsRequest(
        val shard: Int
)

data class GuildsResponse(
        val guilds: List<Guild>) {

    override fun toString() = "GuildsResponse(guilds.size=${guilds.size})"
}

/** Returns [Guild]*/
data class GuildRequest(
        val id: String
)

/** Returns [SendMessageResponse]*/
data class SendMessageRequest(
        val channel: String,
        val content: String
)

data class SendMessageResponse(
        val messageId: String
)

/** Returns [Void]*/
data class SendTypingRequest(
        val channel: String
)

class ApplicationInfoRequest

/** Returns [Boolean]*/
class GuildPermissionRequest(
        val guild: String,
        val role: String? = null,  // If present, the role to check (not mutually exclusive)
        val member: String? = null // If present, the member to check (not mutually exclusive)
)

/** Returns [Boolean]*/
class ChannelPermissionRequest(
        val channel: String, // The channel to check
        val role: String? = null,  // If present, the role to check (not mutually exclusive)
        val member: String? = null // If present, the member to check (not mutually exclusive)
)