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

/* Guild leave/join */
data class GuildJoinEvent(
        val guild: Long
)

data class GuildLeaveEvent(
        val guild: Long,
        val joinTime: Instant
)

/* Guild member events  */
data class GuildMemberJoinEvent(
        val guild: Long,
        val member: Long
)

data class GuildMemberLeaveEvent(
        val guild: Long,
        val member: Long
)

/** When we are subscribed and one of the members change (presence, name, etc) */
data class GuildMemberUpdate(
        val guild: Long,
        val member: Long
)

/* Voice events */
data class VoiceJoinEvent(
        val guild: Long,
        val channel: Long,
        val member: Long
)

data class VoiceLeaveEvent(
        val guild: Long,
        val channel: Long,
        val member: Long
)

data class VoiceMoveEvent(
        val guild: Long,
        val oldChannel: Long,
        val newChannel: Long,
        val member: Long
)

/* Messages */
data class MessageReceivedEvent(
        val id: Long,
        val guild: Long,
        val channel: Long,
        val content: String,
        val author: Long,
        val fromBot: Boolean,
        val attachments: List<String>
)

data class PrivateMessageReceivedEvent(
        val content: String,
        val author: User
)

data class MessageDeleteEvent(
        val id: Long,
        val guild: Long,
        val channel: Long
)
