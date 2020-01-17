/*
 * Copyright © 2018 Frederik Mikkelsen <fred at frederikam.com>
 * FredBoat microservice for handling JDA and Lavalink over RabbitMQ.
 *
 * This program is licensed under GNU AGPLv3 under no warranty.
 */

package com.fredboat.sentinel.jda

import com.fredboat.sentinel.entities.*
import com.fredboat.sentinel.metrics.Counters
import com.fredboat.sentinel.util.Rabbit
import com.fredboat.sentinel.util.toEntity
import com.neovisionaries.ws.client.WebSocketFrame
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.MessageType
import net.dv8tion.jda.api.events.DisconnectEvent
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.ReconnectedEvent
import net.dv8tion.jda.api.events.ResumedEvent
import net.dv8tion.jda.api.events.ShutdownEvent
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.events.channel.category.CategoryDeleteEvent
import net.dv8tion.jda.api.events.channel.category.GenericCategoryEvent
import net.dv8tion.jda.api.events.channel.category.update.CategoryUpdatePermissionsEvent
import net.dv8tion.jda.api.events.channel.category.update.CategoryUpdatePositionEvent
import net.dv8tion.jda.api.events.channel.text.GenericTextChannelEvent
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.voice.GenericVoiceChannelEvent
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdatePositionEvent
import net.dv8tion.jda.api.events.guild.GenericGuildEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent
import net.dv8tion.jda.api.events.guild.update.GuildUpdateOwnerEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.http.HttpRequestEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.api.events.role.GenericRoleEvent
import net.dv8tion.jda.api.events.role.RoleCreateEvent
import net.dv8tion.jda.api.events.role.RoleDeleteEvent
import net.dv8tion.jda.api.events.role.update.RoleUpdatePermissionsEvent
import net.dv8tion.jda.api.events.role.update.RoleUpdatePositionEvent
import net.dv8tion.jda.api.events.user.update.GenericUserPresenceEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.internal.utils.PermissionUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.io.File

@Component
class JdaRabbitEventListener(
        private val rabbit: Rabbit,
        @param:Qualifier("guildSubscriptions")
        private val subscriptions: MutableSet<Long>,
        private val voiceServerUpdateCache: VoiceServerUpdateCache
) : ListenerAdapter() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JdaRabbitEventListener::class.java)
    }

    lateinit var shardManager: ShardManager

    /* Shard lifecycle */

    override fun onStatusChange(event: StatusChangeEvent) = event.run {
        log.info("Shard ${jda.shardInfo}: $oldStatus -> $newStatus")
        dispatch(ShardStatusChange(jda.toEntity()))
    }

    override fun onReady(event: ReadyEvent) {
        dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.READIED))

        if (shardManager.shards.all { it.status == JDA.Status.CONNECTED }) {
            // This file can be used by Ansible playbooks
            File("readyfile").createNewFile()
        }
    }

    override fun onDisconnect(event: DisconnectEvent) {
        dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.DISCONNECTED))

        val frame: WebSocketFrame? = if (event.isClosedByServer)
            event.serviceCloseFrame else event.clientCloseFrame

        val prefix = if (event.isClosedByServer) "s" else "c"
        val code = "$prefix${frame?.closeCode}"
        
        log.warn("Shard ${event.jda.shardInfo} closed. {} {}", code, frame?.closeReason)
        Counters.shardDisconnects.labels(code).inc()
    }

    override fun onResume(event: ResumedEvent) =
            dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.RESUMED))

    override fun onReconnect(event: ReconnectedEvent) =
            dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.RECONNECTED))

    override fun onShutdown(event: ShutdownEvent) =
            dispatch(ShardLifecycleEvent(event.jda.toEntity(), LifecycleEventEnum.SHUTDOWN))

    /* Guild jda */
    override fun onGuildJoin(event: GuildJoinEvent) =
            dispatch(GuildJoinEvent(event.guild.idLong))

    override fun onGuildLeave(event: GuildLeaveEvent) =
            dispatch(GuildLeaveEvent(
                    event.guild.idLong,
                    event.guild.selfMember.timeJoined.toEpochSecond() * 1000
            ))

    /* Member jda */

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
    override fun onGenericUserPresence(event: GenericUserPresenceEvent) = onMemberChange(event.member)
    override fun onGuildMemberUpdateNickname(event: GuildMemberUpdateNicknameEvent) = onMemberChange(event.member)

    private fun onMemberChange(member: net.dv8tion.jda.api.entities.Member) {
        if (!subscriptions.contains(member.guild.idLong)) return
        dispatch(GuildMemberUpdate(
                member.guild.idLong,
                member.toEntity()
        ), print = false)
    }

    /* Voice jda */
    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (!subscriptions.contains(event.guild.idLong)) return
        dispatch(VoiceJoinEvent(
                event.guild.idLong,
                event.channelJoined.idLong,
                event.member.user.idLong
        ))
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        if (event.member.user.idLong == event.guild.selfMember.user.idLong) {
            voiceServerUpdateCache.onVoiceLeave(event.guild.idLong)
        }

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

    /* Message jda */
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
        ), print = false)
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

    private fun updateGuild(event: Event, guild: Guild) {
        log.info("Updated ${guild.id} because of ${event.javaClass.simpleName}")
        dispatch(GuildUpdateEvent(guild.toEntity(voiceServerUpdateCache)))
    }

    private fun updateChannelPermissions(guild: Guild) {
        val permissions = mutableMapOf<String, Long>()
        val self = guild.selfMember
        val func = { channel: GuildChannel ->
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

    private fun dispatch(event: Any, print: Boolean = false) {
        rabbit.sendEvent(event)
        if (print) log.info("Sent $event")
    }

    override fun onHttpRequest(event: HttpRequestEvent) {
        if (event.response?.code ?: -2 >= 300) {
            log.warn("Unsuccessful JDA HTTP Request:\n{}\nResponse:{}\n",
                    event.requestRaw, event.responseRaw)
        }
    }

    override fun onGenericEvent(event: GenericEvent) {
        Counters.jdaEvents.labels(event.javaClass.simpleName).inc()
    }

}