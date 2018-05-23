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
