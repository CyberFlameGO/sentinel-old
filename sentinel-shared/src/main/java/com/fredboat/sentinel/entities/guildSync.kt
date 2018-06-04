package com.fredboat.sentinel.entities

/** Returns [Guild] as well as events */
data class GuildSubscribeRequest(val id: Long)

/** Sent when the [Guild] gets uncached */
data class GuildUnsubscribeRequest(val id: Long)

/** Sent when we need a complete resync on a cached guild */
data class GuildInvalidation(
        val id: Long
)