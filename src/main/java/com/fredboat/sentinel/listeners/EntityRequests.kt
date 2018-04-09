package com.fredboat.sentinel.listeners

import com.fredboat.sentinel.QueueNames
import com.fredboat.sentinel.entities.*
import com.fredboat.sentinel.extension.toEntity
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.TextChannel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = [QueueNames.SENTINEL_REQUESTS_QUEUE])
class EntityRequests(private val shardManager: ShardManager) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(EntityRequests::class.java)
    }

    @RabbitHandler
    fun getGuilds(request: GuildsRequest): GuildsResponse? {
        val jda: JDA? = shardManager.getShardById(request.shard)

        if (jda == null) {
            log.error("Received GuildsRequest for shard ${request.shard} which was not found")
            return null
        } else if (jda.status != JDA.Status.CONNECTED) {
            log.warn("Received GuildsRequest for shard ${request.shard} but status is ${jda.status}")
        }

        val list = mutableListOf<Guild>()
        jda.guilds.forEach { list.add(it.toEntity()) }
        return GuildsResponse(list)
    }

    fun sendMessage(request: SendMessageRequest) {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received SendMessageRequest for channel ${request.channel} which was not found")
            return
        }

        channel.sendMessage(request.content).complete()
    }

    fun sendTyping(request: SendTypingRequest) {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received SendTypingRequest for channel ${request.channel} which was not found")
            return
        }

        channel.sendTyping().queue()
    }

}