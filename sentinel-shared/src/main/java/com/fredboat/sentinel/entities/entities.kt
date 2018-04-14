package com.fredboat.sentinel.entities

data class Shard(
        val id: Int,
        val total: Int,
        val status: ShardStatus
)

data class Guild(
        val id: String,
        val name: String,
        val owner: Member?, // Discord has a history of having guilds without owners :(
        val members: Map<String, Member>,
        val textChannels: List<TextChannel>,
        val voiceChannels: List<VoiceChannel>,
        val roles: List<Role>
)

data class User(
        val id: String,
        val name: String,
        val discrim: Short,
        val bot: Boolean
)

data class Member(
        val id: String,
        val name: String,
        val discrim: Short,
        val guildId: String,
        val bot: Boolean,
        val roles: List<Role>,
        val voiceChannel: VoiceChannel?
)

data class TextChannel(
        val id: String,
        val name: String,
        val ourEffectivePermissions: Long
)

data class VoiceChannel(
        val id: String,
        val name: String,
        val ourEffectivePermissions: Long
)

data class Role(
        val id: String,
        val name: String,
        val permissions: Long
)

data class ApplicationInfo(
        val id: String,
        val requiresCodeGrant: Boolean,
        val description: String,
        val idconId: String,
        val iconUrl: String,
        val name: String,
        val ownerId: String,
        val public: Boolean
)