package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.QueueNames
import com.fredboat.sentinel.entities.*
import com.fredboat.sentinel.entities.ModRequestType.*
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.entities.Icon
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import java.util.*

@Service
@RabbitListener(queues = [QueueNames.SENTINEL_REQUESTS_QUEUE])
class ManagementRequests(private val shardManager: ShardManager) {

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

    @RabbitHandler
    fun receive(request: SetAvatarRequest) {
        val decoded = Base64.getDecoder().decode(request.base64)
        shardManager.shards[0].selfUser.manager.setAvatar(Icon.from(decoded)).complete()
    }

    @RabbitHandler
    fun receive(request: ReviveShardRequest) = shardManager.restart(request.shardId)

    @RabbitHandler
    fun receive(request: LeaveGuildRequest) {
        val guild = shardManager.getGuildById(request.guildId)
                ?: throw RuntimeException("Guild ${request.guildId} not found")
        guild.leave().complete()
    }

    @RabbitHandler
    fun receive(request: GetPingRequest): GetPingReponse {
        val shard = shardManager.getShardById(request.shardId)
        return GetPingReponse(shard?.ping ?: -1, shardManager.averagePing)
    }

}