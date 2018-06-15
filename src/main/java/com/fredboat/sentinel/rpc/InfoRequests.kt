package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.entities.*
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.OnlineStatus
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.stereotype.Service

@Service
class InfoRequests(private val shardManager: ShardManager) {

    @RabbitHandler
    fun consume(request: MemberInfoRequest) {
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

    @RabbitHandler
    fun consume(request: GuildInfoRequest) {
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

    @RabbitHandler
    fun consume(request: RoleInfoRequest) {
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