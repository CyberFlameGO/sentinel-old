package com.fredboat.sentinel.entities

// Additional info about entities, which may be useful in only a few places

data class RoleInfo(
        val id: Long,
        val colorRgb: Int,
        val isHoisted: Boolean,
        val isMentionable: Boolean,
        val isManaged: Boolean
)

data class RoleInfoRequest(val id: Long)

data class GuildInfo(
        val id: Long,
        val iconUrl: String,
        val onlineMembers: Int,
        val verificationLevel: String
)

data class GuildInfoRequest(val id: Long)