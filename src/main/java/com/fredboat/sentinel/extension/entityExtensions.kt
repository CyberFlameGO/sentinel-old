package com.fredboat.sentinel.extension

import com.fredboat.sentinel.entities.*
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.utils.PermissionUtil

fun net.dv8tion.jda.core.JDA.toEntity() = Shard(
        if(shardInfo == null) 0 else shardInfo.shardId,
        if(shardInfo == null) 1 else shardInfo.shardTotal,
        status.toEntity()
)

fun net.dv8tion.jda.core.entities.Guild.toEntity(): Guild {
    val membersMutable = mutableMapOf<String, Member>()
    members.forEach { membersMutable[it.user.id] = it.toEntity() }

    return Guild(
            idLong,
            name,
            owner?.toEntity(),
            membersMutable.toMap(),
            textChannels.map { it.toEntity() },
            voiceChannels.map { it.toEntity() },
            roles.map { it.toEntity() })
}

fun net.dv8tion.jda.core.entities.User.toEntity() = User(
        idLong,
        name,
        discriminator.toShort(),
        isBot)

fun net.dv8tion.jda.core.entities.Member.toEntity(): Member {
    return Member(
            user.idLong,
            user.name,
            nickname,
            user.discriminator.toShort(),
            guild.idLong,
            user.isBot,
            roles.map { it.idLong },
            voiceState?.channel?.idLong)
}

fun net.dv8tion.jda.core.entities.VoiceChannel.toEntity() = VoiceChannel(
        idLong,
        name,
        members.map { it.user.idLong },
        PermissionUtil.getExplicitPermission(this, guild.selfMember))

fun net.dv8tion.jda.core.entities.TextChannel.toEntity() = TextChannel(
        idLong,
        name,
        PermissionUtil.getExplicitPermission(this, guild.selfMember))

fun net.dv8tion.jda.core.entities.Role.toEntity() = Role(
        idLong,
        name,
        permissionsRaw
)

fun JDA.Status.toEntity() = ShardStatus.valueOf(this.toString())