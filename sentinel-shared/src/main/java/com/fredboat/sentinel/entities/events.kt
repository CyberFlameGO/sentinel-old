package com.fredboat.sentinel.entities

/* Shard lifecycle */
data class ShardStatusChange(
        val shard: Shard
)

/* Guild events */
data class GuildJoinEvent(
        val guildId: Long
)

data class GuildLeaveEvent(
        val guildId: Long
)

data class GuildInvalidation(
        val id: Long
)

/* Voice events */
data class VoiceJoinEvent(
        val guildId: Long,
        val chanel: VoiceChannel,
        val member: Member
)

data class VoiceLeaveEvent(
        val guildId: Long,
        val chanel: VoiceChannel,
        val member: Member
)

data class VoiceMoveEvent(
        val guildId: Long,
        val oldChanel: VoiceChannel,
        val newChanel: VoiceChannel,
        val member: Member
)

/* Messages */
data class MessageReceivedEvent(
        val id: Long,
        val guildId: Long,
        val channel: TextChannel,
        val content: String,
        val author: Member
)

data class PrivateMessageReceivedEvent(
        val content: String,
        val author: User
)