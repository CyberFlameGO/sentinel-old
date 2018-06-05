package com.fredboat.sentinel.entities

@Suppress("MemberVisibilityCanBePrivate")
data class Shard(
        val id: Int,
        val total: Int,
        val status: ShardStatus
){
    @Suppress("unused")
    val shardString: String get() = "[$id/$total]"
}

data class Guild(
        val id: Long,
        val name: String,
        val owner: Long?, // Discord has a history of having guilds without owners :(
        val members: List<Member>,
        val textChannels: List<TextChannel>,
        val voiceChannels: List<VoiceChannel>,
        val roles: List<Role>
)

data class User(
        val id: Long,
        val name: String,
        val discrim: Short,
        val bot: Boolean
)

data class Member(
        val id: Long,
        val name: String,
        val nickname: String?,
        val discrim: Short,
        val guildId: Long,
        val bot: Boolean,
        val roles: List<Long>,
        val voiceChannel: Long?
)

data class TextChannel(
        val id: Long,
        val name: String,
        val ourEffectivePermissions: Long
)

data class VoiceChannel(
        val id: Long,
        val name: String,
        val members: List<Long>,
        val userLimit: Int,
        val ourEffectivePermissions: Long
)

data class Role(
        val id: Long,
        val name: String,
        val permissions: Long
)

data class ApplicationInfo(
        val id: Long,
        val botId: Long,
        val requiresCodeGrant: Boolean,
        val description: String,
        val iconId: String,
        val iconUrl: String,
        val name: String,
        val ownerId: Long,
        val public: Boolean
)
