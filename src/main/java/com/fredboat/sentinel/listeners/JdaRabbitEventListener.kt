package com.fredboat.sentinel.listeners

import com.fredboat.sentinel.QueueNames
import com.fredboat.sentinel.entities.*
import com.fredboat.sentinel.extension.toEntity
import net.dv8tion.jda.core.events.DisconnectEvent
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.ReconnectedEvent
import net.dv8tion.jda.core.events.ResumedEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import java.nio.charset.Charset

@Component
class JdaRabbitEventListener(
        private val rabbitTemplate: RabbitTemplate
) : ListenerAdapter() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JdaRabbitEventListener::class.java)
        private val charset = Charset.forName("UTF-8")
    }

    /* Shard lifecycle */
    override fun onReady(event: ReadyEvent) {
        dispatch(ShardReadyEvent(event.jda.shardInfo.shardId, event.jda.shardInfo.shardTotal))
    }

    override fun onDisconnect(event: DisconnectEvent) {
        dispatch(ShardDisconnectedEvent(event.jda.shardInfo.shardId, event.jda.shardInfo.shardTotal))
    }

    override fun onResume(event: ResumedEvent) {
        dispatch(ShardResumedEvent(event.jda.shardInfo.shardId, event.jda.shardInfo.shardTotal))
    }

    override fun onReconnect(event: ReconnectedEvent) {
        dispatch(ShardReconnectedEvent(event.jda.shardInfo.shardId, event.jda.shardInfo.shardTotal))
    }

    /* Guild events */
    override fun onGuildJoin(event: GuildJoinEvent) {
        dispatch(com.fredboat.sentinel.entities.GuildJoinEvent(event.guild.id))
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        dispatch(com.fredboat.sentinel.entities.GuildLeaveEvent(event.guild.id))
    }

    /* Voice events */
    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        dispatch(VoiceJoinEvent(
                event.guild.id,
                event.channelJoined.toEntity(),
                event.member.toEntity()
        ))
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        dispatch(VoiceLeaveEvent(
                event.guild.id,
                event.channelLeft.toEntity(),
                event.member.toEntity()
        ))
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        dispatch(VoiceMoveEvent(
                event.guild.id,
                event.channelLeft.toEntity(),
                event.channelJoined.toEntity(),
                event.member.toEntity()
        ))
    }

    /* Message events */
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        dispatch(MessageReceivedEvent(
                event.message.id,
                event.message.guild.id,
                event.channel.toEntity(),
                event.message.contentRaw,
                event.member.toEntity()
        ))
    }

    override fun onPrivateMessageReceived(event: PrivateMessageReceivedEvent) {
        dispatch(PrivateMessageReceivedEvent(
                event.message.contentRaw,
                event.author.toEntity()
        ))
    }

    private fun dispatch(event: Any) {
        rabbitTemplate.convertAndSend(QueueNames.JDA_EVENTS_QUEUE, event)
        log.info("Sent $event")
        val res = rabbitTemplate.convertSendAndReceive(QueueNames.SENTINEL_REQUESTS_QUEUE, UsersRequest(0)) as UsersResponse
    }


}