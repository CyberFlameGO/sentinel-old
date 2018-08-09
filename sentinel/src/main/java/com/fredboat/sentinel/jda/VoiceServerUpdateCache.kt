package com.fredboat.sentinel.jda

import com.fredboat.sentinel.entities.VoiceServerUpdate
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * When FredBoat restarts and Sentinel remains, something needs to tell FredBoat about our existing voice connections
 * This service takes care of sending updates when receiving them, and when subscribing
 */
@Service
class VoiceServerUpdateCache {

    private val map = ConcurrentHashMap<Long, VoiceServerUpdate>()

    operator fun set(guildId: Long, update: VoiceServerUpdate) = map.put(guildId, update)
    operator fun get(guildId: Long): VoiceServerUpdate? = map[guildId]

    /** Invalidate */
    fun onVoiceLeave(guildId: Long) = map.remove(guildId)

}