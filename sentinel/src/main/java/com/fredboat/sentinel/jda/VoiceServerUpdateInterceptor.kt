/*
 * Copyright © 2018 Frederik Mikkelsen <fred at frederikam.com>
 * FredBoat microservice for handling JDA and Lavalink over RabbitMQ.
 *
 * This program is licensed under GNU AGPLv3 under no warranty.
 */

package com.fredboat.sentinel.jda

import com.fredboat.sentinel.entities.VoiceServerUpdate
import com.fredboat.sentinel.util.Rabbit
import net.dv8tion.jda.core.entities.impl.JDAImpl
import net.dv8tion.jda.core.handle.SocketHandler
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class VoiceServerUpdateInterceptor(
        jda: JDAImpl,
        private val rabbit: Rabbit,
        private val voiceServerUpdateCache: VoiceServerUpdateCache
) : SocketHandler(jda) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(VoiceServerUpdateInterceptor::class.java)
    }

    override fun handleInternally(content: JSONObject): Long? {
        log.debug(content.toString())
        val idLong = content.getLong("guild_id")

        if (jda.guildSetupController.isLocked(idLong))
            return idLong

        // Get session
        val guild = jda.guildMap.get(idLong)
                ?: throw IllegalArgumentException("Attempted to start audio connection with Guild that doesn't exist! JSON: $content")

        val event = VoiceServerUpdate(guild.selfMember.voiceState.sessionId, content.toString())
        voiceServerUpdateCache[idLong] = event

        rabbit.sendEvent(event)

        return null
    }

}