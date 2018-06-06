package com.fredboat.sentinel.events

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.entities.*
import com.fredboat.sentinel.extension.toEntity
import net.dv8tion.jda.core.entities.MessageType
import net.dv8tion.jda.core.entities.impl.JDAImpl
import net.dv8tion.jda.core.events.*
import net.dv8tion.jda.core.events.channel.category.GenericCategoryEvent
import net.dv8tion.jda.core.events.channel.text.GenericTextChannelEvent
import net.dv8tion.jda.core.events.channel.voice.GenericVoiceChannelEvent
import net.dv8tion.jda.core.events.guild.GenericGuildEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.member.*
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.guild.update.GuildUpdateNameEvent
import net.dv8tion.jda.core.events.guild.update.GuildUpdateOwnerEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.http.HttpRequestEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.events.role.RoleCreateEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.events.user.update.GenericUserPresenceEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.utils.PermissionUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class JdaRabbitEventListener(
        private val rabbitTemplate: RabbitTemplate,
        @param:Qualifier("guildSubscriptions")
        private val subscriptions: MutableSet<Long>
) : ListenerAdapter() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JdaRabbitEventListener::class.java)
    }

    /* Shard lifecycle */

    override fun onStatusChange(event: StatusChangeEvent) = event.run {
        log.info("Shard ${jda.shardInfo}: $oldStatus -> $newStatus")
        dispatch(ShardStatusChange(jda.toEntity()))
    }

    override fun onReady(event: ReadyEvent) {
        dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.READIED))

        val handlers = (event.jda as JDAImpl).client.handlers
        handlers["VOICE_SERVER_UPDATE"] = VoiceServerUpdateInterceptor(event.jda as JDAImpl, rabbitTemplate)
        handlers["VOICE_STATE_UPDATE"] = VoiceStateUpdateInterceptor(event.jda as JDAImpl)
    }

    override fun onDisconnect(event: DisconnectEvent) =
            dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.DISCONNECTED))

    override fun onResume(event: ResumedEvent) =
            dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.RESUMED))

    override fun onReconnect(event: ReconnectedEvent) =
            dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.RECONNECTED))

    override fun onShutdown(event: ShutdownEvent) =
            dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.SHUTDOWN))

    /* Guild events */
    override fun onGuildJoin(event: GuildJoinEvent) =
            dispatch(com.fredboat.sentinel.entities.GuildJoinEvent(event.guild.idLong))

    override fun onGuildLeave(event: GuildLeaveEvent) =
            dispatch(com.fredboat.sentinel.entities.GuildLeaveEvent(
                    event.guild.idLong,
                    event.guild.selfMember.joinDate.toInstant()
            ))

    /* Member events */

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (!subscriptions.contains(event.guild.idLong)) return
        dispatch(com.fredboat.sentinel.entities.GuildMemberJoinEvent(
                event.guild.idLong,
                event.member.user.idLong
        ))
    }

    override fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        if (!subscriptions.contains(event.guild.idLong)) return
        dispatch(com.fredboat.sentinel.entities.GuildMemberLeaveEvent(
                event.guild.idLong,
                event.member.user.idLong
        ))
    }

    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) = onMemberChange(event.member)
    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) = onMemberChange(event.member)
    override fun onGenericUserPresence(event: GenericUserPresenceEvent<*>) = onMemberChange(event.member)
    override fun onGuildMemberNickChange(event: GuildMemberNickChangeEvent) = onMemberChange(event.member)

    private fun onMemberChange(member: net.dv8tion.jda.core.entities.Member) {
        if (!subscriptions.contains(member.guild.idLong)) return
        dispatch(GuildMemberUpdate(
                member.guild.idLong,
                member.user.idLong
        ))
    }

    /* Voice events */
    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (!subscriptions.contains(event.guild.idLong)) return
        dispatch(VoiceJoinEvent(
                event.guild.idLong,
                event.channelJoined.idLong,
                event.member.user.idLong
        ))
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        if (!subscriptions.contains(event.guild.idLong)) return
        dispatch(VoiceLeaveEvent(
                event.guild.idLong,
                event.channelLeft.idLong,
                event.member.user.idLong
        ))
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (!subscriptions.contains(event.guild.idLong)) return
        dispatch(VoiceMoveEvent(
                event.guild.idLong,
                event.channelLeft.idLong,
                event.channelJoined.idLong,
                event.member.user.idLong
        ))
    }

    /* Message events */
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) = event.run {
        if (message.type != MessageType.DEFAULT) return

        dispatch(MessageReceivedEvent(
                message.idLong,
                message.guild.idLong,
                channel.idLong,
                PermissionUtil.getEffectivePermission(channel, guild.selfMember),   message.attachments.map { if (it.isImage) it.proxyUrl else it.url }
        ))
    }

    override fun onPrivateMessageReceived(event: PrivateMessageReceivedEvent) {
        dispatch(PrivateMessageReceivedEvent(
                event.message.contentRaw,
                event.author.toEntity()
        ))
    }

    override fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
        dispatch(MessageDeleteEvent(
                event.messageIdLong,
                event.guild.idLong,
                event.channel.idLong
        ))
    }

    /*
    *** Guild invalidation ***

    Things that we don't explicitly handle, but that we cache:

    Guild name and owner
    Roles
    Channels
    Channel names (text, voice, categories)
    Our permissions in channels TODO

    We can improve performance by handling more of these
     */

    override fun onGenericGuild(event: GenericGuildEvent) {
        if (event is GuildUpdateNameEvent
                || event is GuildUpdateOwnerEvent
                || event is RoleCreateEvent
                || event is RoleDeleteEvent) {
            log.info("Invalidated guild because of ${event.javaClass.simpleName}")
            dispatch(GuildInvalidation(event.guild.idLong))
        }
    }

    override fun onGenericTextChannel(event: GenericTextChannelEvent) {
        log.info("Invalidated guild because of ${event.javaClass.simpleName}")
        dispatch(GuildInvalidation(event.guild.idLong))
    }

    override fun onGenericVoiceChannel(event: GenericVoiceChannelEvent) {
        log.info("Invalidated guild because of ${event.javaClass.simpleName}")
        dispatch(GuildInvalidation(event.guild.idLong))
    }

    override fun onGenericCategory(event: GenericCategoryEvent) {
        log.info("Invalidated guild because of ${event.javaClass.simpleName}")
        dispatch(GuildInvalidation(event.guild.idLong))
    }

    /* Util */

    private fun dispatch(event: Any) {
        rabbitTemplate.convertAndSend(SentinelExchanges.EVENTS, event)
        log.info("Sent $event")
    }

    override fun onHttpRequest(event: HttpRequestEvent) {
        if (event.response.code >= 300) {
            log.warn("Unsuccessful JDA HTTP Request:\n{}\nResponse:{}\n",
                    event.requestRaw, event.responseRaw)
        }
    }


}