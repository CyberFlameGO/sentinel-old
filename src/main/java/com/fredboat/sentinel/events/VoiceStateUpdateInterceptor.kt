package com.fredboat.sentinel.events

import net.dv8tion.jda.core.entities.impl.JDAImpl
import net.dv8tion.jda.core.handle.VoiceStateUpdateHandler
import org.json.JSONObject

class VoiceStateUpdateInterceptor(jda: JDAImpl) : VoiceStateUpdateHandler(jda) {

    override fun handleInternally(content: JSONObject): Long? {
        val guildId = if (content.has("guild_id")) content.getLong("guild_id") else null
        if (guildId != null && api.guildLock.isLocked(guildId))
            return guildId
        if (guildId == null)
            return super.handleInternally(content)

        val userId = content.getLong("user_id")
        val channelId = if (!content.isNull("channel_id")) content.getLong("channel_id") else null
        val guild = api.getGuildById(guildId) ?: return super.handleInternally(content)

        val member = guild.getMemberById(userId) ?: return super.handleInternally(content)

        // We only need special handling if our own state is modified
        if (!member.equals(guild.selfMember)) return super.handleInternally(content)

        val channel = if (channelId != null) guild.getVoiceChannelById(channelId) else null

        /* These should be handled by JDA events on FredBoat
        if (channelId == null) {
            // Null channel means disconnected
            if (link.getState() !== Link.State.DESTROYED) {
                link.onDisconnected()
            }
        } else if (channel != null) {
            link.setChannel(channel) // Change expected channel
        }*/

        //if (link.getState() === Link.State.CONNECTED) {
        // This may be problematic
        api.client.updateAudioConnection(guildId, channel)
        //}

        return super.handleInternally(content)
    }

}