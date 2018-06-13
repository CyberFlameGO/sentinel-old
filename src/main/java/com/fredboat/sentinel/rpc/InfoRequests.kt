package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.entities.*
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.OnlineStatus
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = [SentinelExchanges.REQUESTS])
class InfoRequests(private val shardManager: ShardManager) {

    @RabbitHandler
    fun guilds(request: MemberInfoRequest) {
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
    fun guilds(request: GuildInfoRequest) {
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
    fun roles(request: RoleInfoRequest) {
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