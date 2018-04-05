package com.fredboat.sentinel.entities

/* Shard lifecycle */
data class ShardConnectedEvent(
        val id: Int,
        val total: Int
)

data class ShardDisconnectedEvent(
        val id: Int,
        val total: Int
)

data class ShardResumedEvent(
        val id: Int,
        val total: Int
)

data class ShardReconnectedEvent(
        val id: Int,
        val total: Int
)

/* Guild events */
data class GuildJoinEvent(
        val guildId: String
)

data class GuildLeaveEvent(
        val guildId: String
)

/* Voice */
data class VoiceJoinEvent(
        val guildId: String,
        val channelId: String,
        val member: Member
)

data class VoiceLeaveEvent(
        val guildId: String,
        val channelId: String,
        val member: Member
)

data class VoiceMoveEvent(
        val guildId: String,
        val oldChannelId: String,
        val newChannelId: String,
        val member: Member
)

/* Messages */
data class MessageReceivedEvent(
        val id: String,
        val guildId: String,
        val channelId: String,
        val content: String,
        val author: Member
)

data class PrivateMessageReceivedEvent(
        val content: String,
        val author: User
)