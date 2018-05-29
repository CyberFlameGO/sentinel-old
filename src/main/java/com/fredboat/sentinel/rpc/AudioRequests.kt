package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.entities.AudioQueueRequest
import com.fredboat.sentinel.entities.AudioQueueRequestEnum.*
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.impl.JDAImpl
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

/**
 * Requests related to audio
 */
@Service
@RabbitListener(queues = [SentinelExchanges.REQUESTS])
class AudioRequests(private val shardManager: ShardManager) {

    @RabbitHandler
    fun audioQueueRequest(request: AudioQueueRequest) {
        val guild: Guild = shardManager.getGuildById(request.guild)
                ?: throw RuntimeException("Guild ${request.guild} not found")

        val jda = guild.jda as JDAImpl

        when (request.type) {
            REMOVE -> jda.client.removeAudioConnection(request.guild)
            QUEUE_DISCONNECT -> jda.client.queueAudioDisconnect(guild)
            QUEUE_CONNECT -> {
                val vc = guild.getVoiceChannelById(request.channel!!)
                        ?: throw RuntimeException("Channel ${request.channel} not found in guild $guild")

                jda.client.queueAudioConnect(vc)
            }
        }
    }

}