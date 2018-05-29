package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.entities.*
import com.fredboat.sentinel.extension.toJda
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.entities.TextChannel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import java.util.*

@Service
@RabbitListener(queues = [SentinelExchanges.REQUESTS])
class MessageRequests(private val shardManager: ShardManager) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(EntityRequests::class.java)
    }

    @RabbitHandler
    fun sendMessage(request: SendMessageRequest): SendMessageResponse? {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received SendMessageRequest for channel ${request.channel} which was not found")
            return null
        }

        val msg = when (request.message) {
            is Message -> channel.sendMessage((request.message as Message).toJda()).complete()
            is Embed -> channel.sendMessage((request.message as Embed).toJda()).complete()
            else -> throw IllegalArgumentException("Unknown message type $request")
        }

        return SendMessageResponse(msg.idLong)
    }

    @RabbitHandler
    fun sendPrivateMessage(request: SendPrivateMessageRequest): SendMessageResponse? {
        val user = shardManager.getUserById(request.recipient)
        val channel = user.openPrivateChannel().complete(true)

        if (user == null) {
            log.error("User ${request.recipient} was not found when sending private message")
            return null
        }

        val msg = when (request.message) {
            is Message -> channel.sendMessage((request.message as Message).toJda()).complete()
            is Embed -> channel.sendMessage((request.message as Embed).toJda()).complete()
            else -> throw IllegalArgumentException("Unknown message type $request")
        }
        return SendMessageResponse(msg.idLong)
    }

    @RabbitHandler
    fun editMessage(request: EditMessageRequest) {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received EditMessageRequest for channel ${request.channel} which was not found")
            return
        }

        when (request.message) {
            is Message -> channel.editMessageById(request.messageId, (request.message as Message).toJda()).queue()
            is Embed -> channel.editMessageById(request.messageId, (request.message as Embed).toJda()).queue()
            else -> throw IllegalArgumentException("Unknown message type $request")
        }
    }

    @RabbitHandler
    fun deleteMessages(request: MessageDeleteRequest) {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received MessageDeleteRequest for channel ${request.channel} which was not found")
            return
        }

        val list = LinkedList<String>()
        request.messages.forEach { list.add(it.toString()) }
        channel.deleteMessagesByIds(list).queue()
    }

    @RabbitHandler
    fun sendTyping(request: SendTypingRequest) {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received SendTypingRequest for channel ${request.channel} which was not found")
            return
        }

        channel.sendTyping().queue()
    }

}