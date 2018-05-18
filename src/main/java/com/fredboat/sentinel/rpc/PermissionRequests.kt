package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.QueueNames
import com.fredboat.sentinel.entities.*
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.utils.PermissionUtil
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = [QueueNames.SENTINEL_REQUESTS_QUEUE])
class PermissionRequests(private val shardManager: ShardManager) {

    /**
     * Returns true if the Role and/or Member has the given permissions in a Guild
     */
    @RabbitHandler
    fun checkGuildPermissions(request: GuildPermissionRequest): PermissionCheckResponse {
        val guild = shardManager.getGuildById(request.guild)
                ?: throw RuntimeException("Got request for guild which isn't found")

        request.member?.apply {
            val member = guild.getMemberById(this) ?: return PermissionCheckResponse(0, 0, true)
            val effective = PermissionUtil.getEffectivePermission(member)
            return PermissionCheckResponse(effective, getMissing(request.rawPermissions, effective), false)
        }

        // Role must be specified then
        val role = guild.getRoleById(request.role!!) ?: return PermissionCheckResponse(0, 0, true)
        return PermissionCheckResponse(role.permissionsRaw, getMissing(request.rawPermissions, role.permissionsRaw), false)
    }

    /**
     * Returns true if the Role and/or Member has the given permissions in a Channel
     */
    @RabbitHandler
    fun checkChannelPermissions(request: ChannelPermissionRequest): PermissionCheckResponse {
        var channel: Channel? = shardManager.getTextChannelById(request.channel)
                ?: shardManager.getVoiceChannelById(request.channel)
        channel = channel ?: shardManager.getCategoryById(request.channel)
        channel ?: throw RuntimeException("Got request for channel which isn't found")

        val guild = channel.guild

        request.member?.apply {
            val member = guild.getMemberById(this) ?: return PermissionCheckResponse(0, 0, true)
            val effective = PermissionUtil.getEffectivePermission(channel, member)
            return PermissionCheckResponse(
                    effective,
                    getMissing(request.rawPermissions, effective),
                    false
            )
        }

        // Role must be specified then
        val role = guild.getRoleById(request.role!!) ?: return PermissionCheckResponse(0, 0, true)
        val effective = PermissionUtil.getEffectivePermission(channel, role)
        return PermissionCheckResponse(effective, getMissing(request.rawPermissions, effective), false)
    }

    @RabbitHandler
    fun checkGuildPermissionsBulk(request: BulkGuildPermissionRequest): BulkGuildPermissionResponse {
        val guild = shardManager.getGuildById(request.guild)
                ?: throw RuntimeException("Got request for guild which isn't found")

        return BulkGuildPermissionResponse(request.members.map {
            val member = guild.getMemberById(it) ?: return@map null
            PermissionUtil.getEffectivePermission(member)
        })
    }

    /** Performs converse nonimplication */
    private fun getMissing(expected: Long, actual: Long) = (expected.inv() or actual).inv()

}