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
    val textMutable = mutableListOf<TextChannel>()
    val voiceMutable = mutableListOf<VoiceChannel>()
    val rolesMutable = mutableListOf<Role>()

    members.forEach { membersMutable[it.user.id] = it.toEntity() }
    textChannels.forEach { textMutable.add(it.toEntity()) }
    voiceChannels.forEach { voiceMutable.add(it.toEntity()) }
    roles.forEach { rolesMutable.add(it.toEntity()) }

    return Guild(
            idLong,
            name,
            owner?.toEntity(),
            membersMutable,
            textMutable,
            voiceMutable,
            rolesMutable)
}

fun net.dv8tion.jda.core.entities.User.toEntity() = User(
        idLong,
        name,
        discriminator.toShort(),
        isBot)

fun net.dv8tion.jda.core.entities.Member.toEntity(): Member {
    val rolesMutable = mutableListOf<Role>()
    roles.forEach { rolesMutable.add(it.toEntity()) }

    return Member(
            user.idLong,
            effectiveName,
            user.discriminator.toShort(),
            guild.idLong,
            user.isBot,
            rolesMutable,
            voiceState?.channel?.toEntity())
}

fun net.dv8tion.jda.core.entities.VoiceChannel.toEntity() = VoiceChannel(
        idLong,
        name,
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