package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.entities.*
import com.fredboat.sentinel.entities.ModRequestType.*
import com.fredboat.sentinel.extension.toEntityExtended
import com.fredboat.sentinel.extension.toFuture
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.entities.Icon
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.Future

@Service
class ManagementRequests(private val shardManager: ShardManager) {

    fun consume(modRequest: ModRequest): Future<Void> = modRequest.run {
        val guild = shardManager.getGuildById(guildId)
                ?: throw RuntimeException("Guild $guildId not found")
        val control = guild.controller

        val action = when(type) {
            KICK -> control.kick(userId.toString(), reason)
            BAN -> control.ban(userId.toString(), banDeleteDays, reason)
            UNBAN -> control.unban(userId.toString())
        }
        return action.toFuture(type.name.toLowerCase())
    }

    fun consume(request: SetAvatarRequest): Future<Void> {
        val decoded = Base64.getDecoder().decode(request.base64)
        return shardManager.shards[0].selfUser.manager.setAvatar(Icon.from(decoded)).toFuture("setAvatar")
    }

    fun consume(request: ReviveShardRequest) = shardManager.restart(request.shardId)

    fun consume(request: LeaveGuildRequest): Future<Void> {
        val guild = shardManager.getGuildById(request.guildId)
                ?: throw RuntimeException("Guild ${request.guildId} not found")
        return guild.leave().toFuture("leaveGuild")
    }

    fun consume(request: GetPingRequest): GetPingReponse {
        val shard = shardManager.getShardById(request.shardId)
        return GetPingReponse(shard?.ping ?: -1, shardManager.averagePing)
    }

    fun consume(request: SentinelInfoRequest) = shardManager.run { SentinelInfoResponse(
            guildCache.size(),
            roleCache.size(),
            categoryCache.size(),
            textChannelCache.size(),
            voiceChannelCache.size(),
            emoteCache.size(),
            if (request.includeShards) shards.map { it.toEntityExtended() } else null
    )}

    fun consume(request: UserListRequest) = shardManager.userCache.map { it.idLong }

}