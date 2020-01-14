/*
 * Copyright © 2018 Frederik Mikkelsen <fred at frederikam.com>
 * FredBoat microservice for handling JDA and Lavalink over RabbitMQ.
 *
 * This program is licensed under GNU AGPLv3 under no warranty.
 */

package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.entities.GetUserRequest
import com.fredboat.sentinel.entities.GuildInfo
import com.fredboat.sentinel.entities.GuildInfoRequest
import com.fredboat.sentinel.entities.MemberInfo
import com.fredboat.sentinel.entities.MemberInfoRequest
import com.fredboat.sentinel.entities.RoleInfo
import com.fredboat.sentinel.entities.RoleInfoRequest
import com.fredboat.sentinel.entities.User
import com.fredboat.sentinel.rpc.meta.SentinelRequest
import com.fredboat.sentinel.util.mono
import com.fredboat.sentinel.util.toEntity
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
@SentinelRequest
class InfoRequests(private val shardManager: ShardManager) {

    @SentinelRequest
    fun consume(request: MemberInfoRequest): MemberInfo {
        val member = shardManager.getGuildById(request.guildId)?.getMemberById(request.id)
                ?: throw IllegalStateException("Member ${request.id} of ${request.guildId} not found")

        return member.run {
            MemberInfo(
                    user.idLong,
                    guild.idLong,
                    user.avatarUrl,
                    color?.rgb,
                    timeJoined.toInstant().toEpochMilli()
            )
        }
    }

    @SentinelRequest
    fun consume(request: GuildInfoRequest): GuildInfo {
        val guild = shardManager.getGuildById(request.id)
                ?: throw IllegalStateException("Guild ${request.id} not found")

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
                ?: throw IllegalStateException("Role ${request.id} not found")

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