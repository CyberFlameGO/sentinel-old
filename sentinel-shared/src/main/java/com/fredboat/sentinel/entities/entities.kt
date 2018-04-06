package com.fredboat.sentinel.entities

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
        val voiceChannel: VoiceChannel
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