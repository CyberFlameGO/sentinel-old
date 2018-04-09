package com.fredboat.sentinel.entities

data class Shard(
        val id: Int,
        val total: Int,
        val status: String
)

data class Guild(
        val id: String,
        val name: String,
        val owner: Member,
        val members: List<Member>,
        val textChannels: List<TextChannel>,
        val voiceChannels: List<VoiceChannel>
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