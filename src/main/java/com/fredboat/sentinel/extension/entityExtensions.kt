package com.fredboat.sentinel.extension

import com.fredboat.sentinel.entities.Member
import com.fredboat.sentinel.entities.TextChannel
import com.fredboat.sentinel.entities.User
import com.fredboat.sentinel.entities.VoiceChannel
import net.dv8tion.jda.core.utils.PermissionUtil

fun net.dv8tion.jda.core.entities.User.toEntity() = User(
        id,
        name,
        discriminator.toShort(),
        isBot)

fun net.dv8tion.jda.core.entities.Member.toEntity() = Member(
        user.id,
        effectiveName,
        user.discriminator.toShort(),
        user.isBot,
        voiceState?.channel?.toEntity())

fun net.dv8tion.jda.core.entities.VoiceChannel.toEntity() = VoiceChannel(
        id,
        name,
        PermissionUtil.getExplicitPermission(this, guild.selfMember))

fun net.dv8tion.jda.core.entities.TextChannel.toEntity() = TextChannel(
        id,
        name,
        PermissionUtil.getExplicitPermission(this, guild.selfMember))