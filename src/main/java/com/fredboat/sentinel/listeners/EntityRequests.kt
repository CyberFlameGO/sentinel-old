package com.fredboat.sentinel.listeners

import com.fredboat.sentinel.QueueNames
import com.fredboat.sentinel.entities.Guild
import com.fredboat.sentinel.entities.GuildsRequest
import com.fredboat.sentinel.entities.GuildsResponse
import com.fredboat.sentinel.extension.toEntity
import net.dv8tion.jda.bot.sharding.ShardManager
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = [QueueNames.SENTINEL_REQUESTS_QUEUE])
class EntityRequests(private val shardManager: ShardManager) {

    @RabbitHandler
    fun getGuilds(request: GuildsRequest): GuildsResponse {
        val list = mutableListOf<Guild>()
        shardManager.getShardById(request.shard).guilds.forEach { list.add(it.toEntity()) }
        return GuildsResponse(list)
    }

}