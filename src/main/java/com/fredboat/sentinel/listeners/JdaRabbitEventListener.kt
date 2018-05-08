package com.fredboat.sentinel.listeners

import com.fredboat.sentinel.QueueNames
import com.fredboat.sentinel.entities.*
import com.fredboat.sentinel.extension.toEntity
import net.dv8tion.jda.core.entities.MessageType
import net.dv8tion.jda.core.events.*
import net.dv8tion.jda.core.events.guild.GenericGuildEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class JdaRabbitEventListener(
        private val rabbitTemplate: RabbitTemplate
) : ListenerAdapter() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JdaRabbitEventListener::class.java)
    }

    /* Shard lifecycle */

    override fun onStatusChange(event: StatusChangeEvent) {
        dispatch(ShardStatusChange(event.jda.toEntity()))
    }

    override fun onReady(event: ReadyEvent) =
            dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.READY))
    override fun onDisconnect(event: DisconnectEvent) =
            dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.DISCONNECT))
    override fun onResume(event: ResumedEvent) =
            dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.RESUMED))
    override fun onReconnect(event: ReconnectedEvent) =
            dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.RECONNECT))
    override fun onShutdown(event: ShutdownEvent) =
            dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.SHUTDOWN))

    /* Guild events */
    override fun onGuildJoin(event: GuildJoinEvent) {
        dispatch(com.fredboat.sentinel.entities.GuildJoinEvent(event.guild.idLong))
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        dispatch(com.fredboat.sentinel.entities.GuildLeaveEvent(event.guild.idLong))
    }

    /* Voice events */
    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        dispatch(VoiceJoinEvent(
                event.guild.idLong,
                event.channelJoined.toEntity(),
                event.member.toEntity()
        ))
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        dispatch(VoiceLeaveEvent(
                event.guild.idLong,
                event.channelLeft.toEntity(),
                event.member.toEntity()
        ))
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        dispatch(VoiceMoveEvent(
                event.guild.idLong,
                event.channelLeft.toEntity(),
                event.channelJoined.toEntity(),
                event.member.toEntity()
        ))
    }

    /* Message events */
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.message.type != MessageType.DEFAULT) return

        dispatch(MessageReceivedEvent(
                event.message.idLong,
                event.message.guild.idLong,
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

    /* Guild invalidation */

    override fun onGenericGuild(event: GenericGuildEvent) {
        // Ignore message events
        if (event !is GenericGuildMessageEvent) {
            dispatch(GuildInvalidation(event.guild.idLong))
        }
    }

    /* Util */

    private fun dispatch(event: Any) {
        rabbitTemplate.convertAndSend(QueueNames.JDA_EVENTS_QUEUE, event)
        log.info("Sent $event")
    }


}