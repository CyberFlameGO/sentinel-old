package com.fredboat.sentinel.events

import com.fredboat.sentinel.entities.*
import com.fredboat.sentinel.extension.toEntity
import com.fredboat.sentinel.metrics.Counters
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageType
import net.dv8tion.jda.core.entities.impl.JDAImpl
import net.dv8tion.jda.core.events.*
import net.dv8tion.jda.core.events.channel.category.CategoryDeleteEvent
import net.dv8tion.jda.core.events.channel.category.GenericCategoryEvent
import net.dv8tion.jda.core.events.channel.category.update.CategoryUpdatePermissionsEvent
import net.dv8tion.jda.core.events.channel.category.update.CategoryUpdatePositionEvent
import net.dv8tion.jda.core.events.channel.text.GenericTextChannelEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.GenericVoiceChannelEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.update.VoiceChannelUpdatePositionEvent
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
import net.dv8tion.jda.core.events.role.GenericRoleEvent
import net.dv8tion.jda.core.events.role.RoleCreateEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.events.role.update.RoleUpdatePermissionsEvent
import net.dv8tion.jda.core.events.role.update.RoleUpdatePositionEvent
import net.dv8tion.jda.core.events.user.update.GenericUserPresenceEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.utils.PermissionUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class JdaRabbitEventListener(
        private val rabbitTemplate: RabbitTemplate,
        @param:Qualifier("guildSubscriptions")
        private val subscriptions: MutableSet<Long>,
        @param:Qualifier("eventExchange")
        private val eventsExchange: DirectExchange
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
                event.member.toEntity()
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
                member.toEntity()
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
                PermissionUtil.getEffectivePermission(channel, guild.selfMember),
                message.contentRaw,
                author.idLong,
                author.isBot,
                message.attachments.map { if (it.isImage) it.proxyUrl else it.url }
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
    Our permissions in channels

    We can improve performance by handling more of these
     */

    override fun onGenericGuild(event: GenericGuildEvent) {
        if (!subscriptions.contains(event.guild.idLong)) return
        if (event is GuildUpdateNameEvent
                || event is GuildUpdateOwnerEvent) {
            updateGuild(event, event.guild)
        }
    }

    override fun onGenericTextChannel(event: GenericTextChannelEvent) {
        if (!subscriptions.contains(event.guild.idLong)) return
        if (event is TextChannelDeleteEvent || event is TextChannelCreateEvent) {
            updateGuild(event, event.guild)
            return
        } else if (event is RoleUpdatePositionEvent || event is RoleUpdatePermissionsEvent) {
            updateChannelPermissions(event.guild)
        }

        dispatch(TextChannelUpdate(
                event.guild.idLong,
                event.channel.toEntity()
        ))
    }

    /** Note: voice state updates (join, move, leave, etc) are not handled as [GenericVoiceChannelEvent] */
    override fun onGenericVoiceChannel(event: GenericVoiceChannelEvent) {
        if (!subscriptions.contains(event.guild.idLong)) return
        if (event is VoiceChannelDeleteEvent || event is VoiceChannelCreateEvent) {
            updateGuild(event, event.guild)
            return
        } else if (event is VoiceChannelUpdatePositionEvent || event is RoleUpdatePermissionsEvent) {
            updateChannelPermissions(event.guild)
        }



        dispatch(VoiceChannelUpdate(
                event.guild.idLong,
                event.channel.toEntity()
        ))
    }

    override fun onGenericCategory(event: GenericCategoryEvent) {
        if (!subscriptions.contains(event.guild.idLong)) return
        if (event is CategoryDeleteEvent
                || event is CategoryUpdatePositionEvent
                || event is CategoryUpdatePermissionsEvent) {
            updateGuild(event, event.guild)
        }
    }

    override fun onGenericRole(event: GenericRoleEvent) {
        if (!subscriptions.contains(event.guild.idLong)) return
        if (event is RoleCreateEvent || event is RoleDeleteEvent) {
            updateGuild(event, event.guild)
            return
        } else if (event is RoleUpdatePermissionsEvent || event is RoleUpdatePositionEvent) {
            updateChannelPermissions(event.guild)
        }
        dispatch(RoleUpdate(
                event.guild.idLong,
                event.role.toEntity()
        ))
    }

    private fun updateGuild(event: Event, guild: net.dv8tion.jda.core.entities.Guild) {
        log.info("Updated ${guild.id} because of ${event.javaClass.simpleName}")
        dispatch(GuildUpdateEvent(guild.toEntity()))
    }

    private fun updateChannelPermissions(guild: Guild) {
        val permissions = mutableMapOf<String, Long>()
        val self = guild.selfMember
        val func = { channel: Channel ->
            permissions[channel.id] = PermissionUtil.getEffectivePermission(channel, self)
        }

        guild.textChannels.forEach(func)
        guild.voiceChannels.forEach(func)

        dispatch(ChannelPermissionsUpdate(
                guild.idLong,
                permissions

        ))
    }

    /* Util */

    private fun dispatch(event: Any) {
        rabbitTemplate.convertAndSend(eventsExchange.name, event)
        log.info("Sent $event")
    }

    override fun onHttpRequest(event: HttpRequestEvent) {
        if (event.response.code >= 300) {
            log.warn("Unsuccessful JDA HTTP Request:\n{}\nResponse:{}\n",
                    event.requestRaw, event.responseRaw)
        }
    }

    override fun onGenericEvent(event: Event) {
        Counters.jdaEvents.labels(event.javaClass.simpleName).inc()
    }


}