package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.entities.*
import com.fredboat.sentinel.extension.queue
import com.fredboat.sentinel.extension.toFuture
import com.fredboat.sentinel.extension.toJda
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.entities.TextChannel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
class MessageRequests(private val shardManager: ShardManager) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MessageRequests::class.java)
    }

    fun consume(request: SendMessageRequest): CompletableFuture<SendMessageResponse>? {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received SendMessageRequest for channel ${request.channel} which was not found")
            return null
        }

        return channel.sendMessage(request.message)
                .toFuture("sendMessage")
                .thenApply { SendMessageResponse(it.idLong) }
    }

    fun consume(request: SendEmbedRequest): CompletableFuture<SendMessageResponse>? {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received SendEmbedRequest for channel ${request.channel} which was not found")
            return null
        }

        return channel.sendMessage(request.embed.toJda())
                .toFuture("sendEmbed")
                .thenApply { SendMessageResponse(it.idLong) }
    }

    fun consume(request: SendPrivateMessageRequest): CompletableFuture<SendMessageResponse>? {
        val user = shardManager.getUserById(request.recipient)
        val channel = user.openPrivateChannel().complete(true)

        if (user == null) {
            log.error("User ${request.recipient} was not found when sending private message")
            return null
        }

        return channel.sendMessage(request.message)
                .toFuture("sendPrivate")
                .thenApply { SendMessageResponse(it.idLong) }
    }

    fun consume(request: EditMessageRequest) {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received EditMessageRequest for channel ${request.channel} which was not found")
            return
        }

        channel.editMessageById(request.messageId, request.message).queue("editMessage")
    }

    fun consume(request: MessageDeleteRequest) {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received MessageDeleteRequest for channel ${request.channel} which was not found")
            return
        }

        val list = LinkedList<String>()
        request.messages.forEach { list.add(it.toString()) }
        channel.deleteMessagesByIds(list).queue("deleteMessage")
    }

    fun consume(request: SendTypingRequest) {
        val channel: TextChannel? = shardManager.getTextChannelById(request.channel)

        if (channel == null) {
            log.error("Received SendTypingRequest for channel ${request.channel} which was not found")
            return
        }

        channel.sendTyping().queue("sendTyping")
    }

}