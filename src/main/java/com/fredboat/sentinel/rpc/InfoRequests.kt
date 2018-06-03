package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.entities.RoleInfo
import com.fredboat.sentinel.entities.RoleInfoRequest
import net.dv8tion.jda.bot.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = [SentinelExchanges.REQUESTS])
class InfoRequests(private val shardManager: ShardManager) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(InfoRequests::class.java)
    }

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

}