package com.fredboat.sentinel.extension

import com.fredboat.sentinel.entities.*
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.utils.PermissionUtil

fun net.dv8tion.jda.core.entities.User.toEntity() = User(
        id,
        name,
        discriminator.toShort(),
        isBot)

fun net.dv8tion.jda.core.entities.Member.toEntity(): Member {
    val rolesMutable = mutableListOf<Role>()
    roles.forEach { rolesMutable.add(it.toEntity()) }

    return Member(
            user.id,
            effectiveName,
            user.discriminator.toShort(),
            user.isBot,
            rolesMutable,
            voiceState?.channel?.toEntity())
}

fun net.dv8tion.jda.core.entities.VoiceChannel.toEntity() = VoiceChannel(
        id,
        name,
        PermissionUtil.getExplicitPermission(this, guild.selfMember))

fun net.dv8tion.jda.core.entities.TextChannel.toEntity() = TextChannel(
        id,
        name,
        PermissionUtil.getExplicitPermission(this, guild.selfMember))

fun net.dv8tion.jda.core.entities.Role.toEntity() = Role(
        id,
        name,
        permissionsRaw
)

fun JDA.Status.toEntity() = ShardStatus.valueOf(this.toString())