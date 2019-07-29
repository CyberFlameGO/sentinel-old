/*
 * Copyright © 2018 Frederik Mikkelsen <fred at frederikam.com>
 * FredBoat microservice for handling JDA and Lavalink over RabbitMQ.
 *
 * This program is licensed under GNU AGPLv3 under no warranty.
 */

package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.entities.*
import com.fredboat.sentinel.rpc.meta.SentinelRequest
import com.fredboat.sentinel.util.mono
import com.fredboat.sentinel.util.toJda
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.impl.JDAImpl
import net.dv8tion.jda.core.entities.impl.UserImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class MessageRequests(private val shardManager: ShardManager) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MessageRequests::class.java)
    }

    @SentinelRequest
    fun consume(request: SendMessageRequest): Mono<SendMessageResponse> {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received SendMessageRequest for channel ${request.channel} which was not found")
            return Mono.empty()
        }

        return channel.sendMessage(request.message)
                .mono("sendMessage")
                .map { SendMessageResponse(it.idLong) }
    }

    @SentinelRequest
    fun consume(request: SendEmbedRequest): Mono<SendMessageResponse> {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received SendEmbedRequest for channel ${request.channel} which was not found")
            return Mono.empty()
        }

        return channel.sendMessage(request.embed.toJda()).mono("sendEmbed").map {
            SendMessageResponse(it.idLong)
        }
    }

    @SentinelRequest
    fun consume(request: SendPrivateMessageRequest): Mono<SendMessageResponse> {
        val shard = shardManager.shards.find { it.status == JDA.Status.CONNECTED } as JDAImpl
        val user = UserImpl(request.recipient, shard)

        return user.openPrivateChannel()
                .mono("openPrivateChannel")
                .flatMap { it.sendMessage(request.message).mono("sendPrivateMessage") }
                .map { SendMessageResponse(it.idLong) }
    }

    @SentinelRequest
    fun consume(request: EditMessageRequest): Mono<Void> {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received EditMessageRequest for channel ${request.channel} which was not found")
            return Mono.empty()
        }

        return channel.editMessageById(request.messageId, request.message)
                .mono("editMessage").then()
    }

    @SentinelRequest
    fun consume(request: MessageDeleteRequest): Mono<Void> {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received MessageDeleteRequest for channel ${request.channel} which was not found")
            return Mono.empty()
        }

        if (request.messages.size < 2) {
            return channel.deleteMessageById(request.messages[0].toString())
                    .mono("deleteMessage")
                    .then()
        }

        val list = request.messages.map { toString() }
        return channel.deleteMessagesByIds(list).mono("deleteMessages")
    }

    @SentinelRequest
    fun consume(request: SendTypingRequest): Mono<Void> {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received SendTypingRequest for channel ${request.channel} which was not found")
            return Mono.empty()
        }

        return channel.sendTyping().mono("sendTyping")
    }

}