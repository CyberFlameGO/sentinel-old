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
        val id: Long
)

/** Returns [SendMessageResponse]*/
data class SendMessageRequest(
        val channel: Long,
        val message: IMessage
)

/** Returns [SendMessageResponse]*/
data class SendPrivateMessageRequest(
        val recipient: Long,
        val message: IMessage
)

/** Returns [Unit]*/
data class EditMessageRequest(
        val channel: Long,
        val messageId: Long,
        val message: IMessage
)

data class SendMessageResponse(
        val messageId: Long
)

/** Returns [Unit]*/
data class MessageDeleteRequest(
        val channel: Long,
        val messages: List<Long>
)

/** Returns [Unit]*/
data class SendTypingRequest(
        val channel: Long
)

/** Returns [ApplicationInfo]*/
class ApplicationInfoRequest

/** Returns [PermissionCheckResponse]*/
data class GuildPermissionRequest(
        val guild: Long,
        val role: Long? = null,  // If present, the role to check (mutually exclusive)
        val member: Long? = null,// If present, the member to check (mutually exclusive)
        val rawPermissions: Long
){
    init {
        if (role != null && member != null) throw RuntimeException("Role and member are mutually exclusive")
    }
}

/** Returns [PermissionCheckResponse]*/
data class ChannelPermissionRequest(
        val channel: Long, // The channel to check
        val role: Long? = null,  // If present, the role to check (mutually exclusive)
        val member: Long? = null,// If present, the member to check (mutually exclusive)
        val rawPermissions: Long
){
    init {
        if (role != null && member != null) throw RuntimeException("Role and member are mutually exclusive")
    }
}

data class PermissionCheckResponse(
        val effective: Long,
        val missing: Long,
        val missingEntityFault: Boolean
) {
    @Suppress("unused")
    val passed: Boolean
        get() = !missingEntityFault && missing == 0L
}

/** Returns [BulkGuildPermissionRequest]*/
data class BulkGuildPermissionRequest(
        val guild: Long,
        val members: List<Long>
)

data class BulkGuildPermissionResponse(
        val effectivePermissions: List<Long?>
)

/** Returns [SentinelInfoResponse] */
data class SentinelInfoRequest(val includeShards: Boolean = false)

/** Data about all shards */
data class SentinelInfoResponse(
        val guilds: Long,
        val roles: Long,
        val categories: Long,
        val textChannels: Long,
        val voiceChannels: Long,
        val emotes: Long,
        val shards: List<ExtendedShardInfo>?
)

/** For the ;;shards command */
data class ExtendedShardInfo(
        val shard: Shard,
        val guilds: Int,
        val users: Int
)

/** Dump all user IDs to a [List] with [String]s for faster encoding/decoding */
class UserListRequest

