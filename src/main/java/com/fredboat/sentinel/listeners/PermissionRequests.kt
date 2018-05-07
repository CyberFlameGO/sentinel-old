package com.fredboat.sentinel.listeners

import com.fredboat.sentinel.QueueNames
import com.fredboat.sentinel.entities.ChannelPermissionRequest
import com.fredboat.sentinel.entities.GuildPermissionRequest
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = [QueueNames.SENTINEL_REQUESTS_QUEUE])
class PermissionRequests(private val shardManager: ShardManager) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PermissionRequests::class.java)
    }

    /**
     * Returns true if the Role and/or Member has the given permissions in a Guild
     */
    @RabbitHandler
    fun checkGuildPermissions(request: GuildPermissionRequest): Boolean {
        val guild = shardManager.getGuildById(request.guild)
                ?: throw RuntimeException("Got request for guild which isn't found")

        request.member?.apply {
            val member = guild.getMemberById(this) ?: return false
            if (!member.hasPermission(Permission.toEnumSet(request.rawPermissions))) return false
        }

        request.role?.apply {
            val role = guild.getRoleById(this) ?: return false
            if (!role.hasPermission(Permission.toEnumSet(request.rawPermissions))) return false
        }

        return true
    }

    /**
     * Returns true if the Role and/or Member has the given permissions in a Channel
     */
    @RabbitHandler
    fun checkChannelPermissions(request: ChannelPermissionRequest): Boolean {
        var channel: Channel? = shardManager.getTextChannelById(request.channel)
                ?: shardManager.getVoiceChannelById(request.channel)
        channel = channel ?: shardManager.getCategoryById(request.channel)
        channel ?: throw RuntimeException("Got request for channel which isn't found")

        val guild = channel.guild

        // These two apply blocks are the same as the two above, but with the channel
        request.member?.apply {
            val member = guild.getMemberById(this) ?: return false
            if (!member.hasPermission(channel, Permission.toEnumSet(request.rawPermissions))) return false
        }

        request.role?.apply {
            val role = guild.getRoleById(this) ?: return false
            if (!role.hasPermission(channel, Permission.toEnumSet(request.rawPermissions))) return false
        }

        return true
    }

}