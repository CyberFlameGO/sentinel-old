package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.entities.*
import com.fredboat.sentinel.entities.ModRequestType.*
import com.fredboat.sentinel.extension.complete
import com.fredboat.sentinel.extension.queue
import com.fredboat.sentinel.extension.toEntity
import com.fredboat.sentinel.extension.toEntityExtended
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.entities.Icon
import org.springframework.stereotype.Service
import java.util.*

@Service
class ManagementRequests(private val shardManager: ShardManager) {

    fun consume(modRequest: ModRequest) = modRequest.run {
        val guild = shardManager.getGuildById(guildId)
                ?: throw RuntimeException("Guild $guildId not found")
        val control = guild.controller

        val action = when(type) {
            KICK -> control.kick(userId.toString(), reason)
            BAN -> control.ban(userId.toString(), banDeleteDays, reason)
            UNBAN -> control.unban(userId.toString())
        }
        action.queue(type.name.toLowerCase())
    }

    fun consume(request: SetAvatarRequest) {
        val decoded = Base64.getDecoder().decode(request.base64)
        shardManager.shards[0].selfUser.manager.setAvatar(Icon.from(decoded)).queue("setAvatar")
    }

    fun consume(request: ReviveShardRequest) = shardManager.restart(request.shardId)

    fun consume(request: LeaveGuildRequest) {
        val guild = shardManager.getGuildById(request.guildId)
                ?: throw RuntimeException("Guild ${request.guildId} not found")
        guild.leave().queue("leaveGuild")
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

    fun consume(request: BanListRequest): List<Ban> {
        val guild = shardManager.getGuildById(request.guildId)
                ?: throw RuntimeException("Guild ${request.guildId} not found")
        return guild.banList.complete("getBanList").map {
            Ban(it.user.toEntity(), it.reason)
        }
    }

}