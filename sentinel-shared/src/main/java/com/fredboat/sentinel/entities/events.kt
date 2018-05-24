package com.fredboat.sentinel.entities

import java.time.Instant

/* Shard lifecycle */
data class ShardStatusChange(
        val shard: Shard
)

data class ShardLifecycleEvent(
        val shard: Shard,
        val change: LifecycleEventEnum
)

enum class LifecycleEventEnum {
    READIED,
    DISCONNECTED,
    RESUMED,
    RECONNECTED,
    SHUTDOWN
}

/* Guild events */
data class GuildJoinEvent(
        val guildId: Long
)

data class GuildLeaveEvent(
        val guildId: Long,
        val joinTime: Instant
)

data class GuildInvalidation(
        val id: Long
)

/* Voice events */
data class VoiceJoinEvent(
        val guildId: Long,
        val channel: VoiceChannel,
        val member: Member
)

data class VoiceLeaveEvent(
        val guildId: Long,
        val channel: VoiceChannel,
        val member: Member
)

data class VoiceMoveEvent(
        val guildId: Long,
        val oldChannel: VoiceChannel,
        val newChannel: VoiceChannel,
        val member: Member
)

/* Messages */
data class MessageReceivedEvent(
        val id: Long,
        val guildId: Long,
        val channel: TextChannel,
        val content: String,
        val author: Member,
        val attachments: List<String>
)

data class PrivateMessageReceivedEvent(
        val content: String,
        val author: User
)

data class MessageDeleteEvent(
        val id: Long,
        val guildId: Long,
        val channel: TextChannel
)
