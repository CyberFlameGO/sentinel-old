package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.entities.GuildInfo
import com.fredboat.sentinel.entities.GuildInfoRequest
import com.fredboat.sentinel.entities.RoleInfo
import com.fredboat.sentinel.entities.RoleInfoRequest
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.OnlineStatus
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = [SentinelExchanges.REQUESTS])
class InfoRequests(private val shardManager: ShardManager) {

    @RabbitHandler
    fun roles(request: RoleInfoRequest) {
        val role = shardManager.getRoleById(request.id)
        return role.run {
            RoleInfo(
                    idLong,
                    color.rgb,
                    isHoisted,
                    isMentionable,
                    isManaged
            )
        }
    }

    @RabbitHandler
    fun guilds(request: GuildInfoRequest) {
        val guild = shardManager.getGuildById(request.id)
        return guild.run {
            GuildInfo(
                    idLong,
                    guild.memberCache.count { it.onlineStatus != OnlineStatus.OFFLINE },
                    verificationLevel.name
            )
        }
    }

}