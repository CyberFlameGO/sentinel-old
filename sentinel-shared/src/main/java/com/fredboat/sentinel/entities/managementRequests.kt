package com.fredboat.sentinel.entities

/* This file contains requests for managing either Sentinel or Guilds (banning, reviving, etc) */

data class ModRequest(
        val guildId: Long,
        val userId: Long,
        val type: ModRequestType,
        val reason: String = "",
        val banDeleteDays: Int = 0
)

enum class ModRequestType { KICK, BAN, UNBAN }

data class SetAvatarRequest(val base64: String)

data class ReviveShardRequest(val shardId: Int)

data class LeaveGuildRequest(val guildId: Long)

/** Returns the ping time of JDA's websocket and the shard manager average in milliseconds with [GetPingReponse]*/
data class GetPingRequest(val shardId: Int)

data class GetPingReponse(val shardPing: Long, val average: Double)

/** Responds with [List] of [Ban]*/
data class BanListRequest(val guildId: Long)
data class Ban(val user: User, val reason: String?)