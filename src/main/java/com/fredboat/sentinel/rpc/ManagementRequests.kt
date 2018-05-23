package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.QueueNames
import com.fredboat.sentinel.entities.ModRequest
import com.fredboat.sentinel.entities.ModRequestType.*
import net.dv8tion.jda.bot.sharding.DefaultShardManager
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = [QueueNames.SENTINEL_REQUESTS_QUEUE])
class ManagementRequests(private val shardManager: DefaultShardManager) {

    @RabbitHandler
    fun receive(modRequest: ModRequest) = modRequest.apply {
        val guild = shardManager.getGuildById(guildId)
                ?: throw RuntimeException("Guild $guildId not found")
        val control = guild.controller

        val action = when(type) {
            KICK -> control.kick(userId.toString(), reason)
            BAN -> control.ban(userId.toString(), banDeleteDays, reason)
            UNBAN -> control.unban(userId.toString())
        }

        action.complete()
    }

}