package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.entities.*
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.OnlineStatus
import org.springframework.stereotype.Service

@Service
class InfoRequests(private val shardManager: ShardManager) {

    fun consume(request: MemberInfoRequest): MemberInfo {
        val member = shardManager.getGuildById(request.guildId).getMemberById(request.id)
        return member.run {
            MemberInfo(
                    user.idLong,
                    guild.idLong,
                    user.avatarUrl,
                    color.rgb,
                    joinDate.toInstant().toEpochMilli()
            )
        }
    }

    fun consume(request: GuildInfoRequest): GuildInfo {
        val guild = shardManager.getGuildById(request.id)
        return guild.run {
            GuildInfo(
                    idLong,
                    guild.iconUrl,
                    guild.memberCache.count { it.onlineStatus != OnlineStatus.OFFLINE },
                    verificationLevel.name
            )
        }
    }

    fun consume(request: RoleInfoRequest): RoleInfo {
        val role = shardManager.getRoleById(request.id)
        return role.run {
            RoleInfo(
                    idLong,
                    position,
                    color.rgb,
                    isHoisted,
                    isMentionable,
                    isManaged
            )
        }
    }

}