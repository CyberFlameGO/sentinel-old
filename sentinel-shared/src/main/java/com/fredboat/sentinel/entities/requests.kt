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
        val content: String
)

data class SendMessageResponse(
        val messageId: Long
)

/** Returns [Void]*/
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
        val missingPermissions: Long,
        val missingEntityFault: Boolean
) {
    val passed: Boolean
        get() = !missingEntityFault && missingPermissions == 0L
}