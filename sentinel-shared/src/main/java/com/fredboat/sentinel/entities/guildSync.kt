package com.fredboat.sentinel.entities

/** Returns [Guild] as well as jda */
data class GuildSubscribeRequest(
        val id: Long,
        /** Optionally a channel to post a warning in, if the guild is very large */
        val channelInvoked: Long?
)

/** Sent when the [Guild] gets uncached */
data class GuildUnsubscribeRequest(val id: Long)

data class GuildUpdateEvent(
        val guild: Guild
)

/* Updates */


/** When we are subscribed and one of the members change (presence, name, etc) */
data class GuildMemberUpdate(
        val guild: Long,
        val member: Member
)

data class RoleUpdate(
        val guild: Long,
        val role: Role
)

data class TextChannelUpdate(
        val guild: Long,
        val channel: TextChannel
)

data class VoiceChannelUpdate(
        val guild: Long,
        val channel: VoiceChannel
)

data class ChannelPermissionsUpdate(
        val guild: Long,
        val changes: Map<String, Long>
)