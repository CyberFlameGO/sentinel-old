package com.fredboat.sentinel.entities

/* Shard lifecycle */
data class ShardStatusChange(
        val shard: Shard
)

/* Guild events */
data class GuildJoinEvent(
        val guildId: String
)

data class GuildLeaveEvent(
        val guildId: String
)

/* Voice events */
data class VoiceJoinEvent(
        val guildId: String,
        val chanel: VoiceChannel,
        val member: Member
)

data class VoiceLeaveEvent(
        val guildId: String,
        val chanel: VoiceChannel,
        val member: Member
)

data class VoiceMoveEvent(
        val guildId: String,
        val oldChanel: VoiceChannel,
        val newChanel: VoiceChannel,
        val member: Member
)

/* Messages */
data class MessageReceivedEvent(
        val id: String,
        val guildId: String,
        val channel: TextChannel,
        val content: String,
        val author: Member
)

data class PrivateMessageReceivedEvent(
        val content: String,
        val author: User
)

data class GuildInvalidation(
        val id: String
)
