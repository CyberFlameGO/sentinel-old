/*
 * Copyright © 2018 Frederik Mikkelsen <fred at frederikam.com>
 * FredBoat microservice for handling JDA and Lavalink over RabbitMQ.
 *
 * This program is licensed under GNU AGPLv3 under no warranty.
 */

package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.entities.*
import com.fredboat.sentinel.rpc.meta.SentinelRequest
import com.fredboat.sentinel.util.mono
import com.fredboat.sentinel.util.toEntity
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.exceptions.ErrorResponseException
import net.dv8tion.jda.core.requests.ErrorResponse
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
@SentinelRequest
class InfoRequests(private val shardManager: ShardManager) {

    @SentinelRequest
    fun consume(request: MemberInfoRequest): MemberInfo {
        val member = shardManager.getGuildById(request.guildId).getMemberById(request.id)
        return member.run {
            MemberInfo(
                    user.idLong,
                    guild.idLong,
                    user.avatarUrl,
                    color?.rgb,
                    joinDate.toInstant().toEpochMilli()
            )
        }
    }

    @SentinelRequest
    fun consume(request: GuildInfoRequest): GuildInfo {
        val guild = shardManager.getGuildById(request.id)
        return guild.run {
            GuildInfo(
                    idLong,
                    guild.iconUrl,
                    guild.memberCache.count { it.onlineStatus != OnlineStatus.OFFLINE },
                    verificationLevel.name
            )
        }
    }

    @SentinelRequest
    fun consume(request: RoleInfoRequest): RoleInfo {
        val role = shardManager.getRoleById(request.id)
        return role.run {
            RoleInfo(
                    idLong,
                    position,
                    color?.rgb,
                    isHoisted,
                    isMentionable,
                    isManaged
            )
        }
    }

    @SentinelRequest
    fun consume(request: GetUserRequest): Mono<User> = shardManager.retrieveUserById(request.id)
            .mono(("fetchUser"))
            .onErrorContinue { t, _ ->
                // Just drop the user if it was not found. Fail otherwise.
                t is ErrorResponseException && t.errorResponse == ErrorResponse.UNKNOWN_USER
            }.map { it.toEntity() }

}