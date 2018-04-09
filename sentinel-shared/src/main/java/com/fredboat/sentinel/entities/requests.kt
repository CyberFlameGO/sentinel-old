package com.fredboat.sentinel.entities

data class GuildsRequest(
        val shard: Int
)

data class GuildsResponse(
        val guilds: List<Guild>) {

    override fun toString() = "GuildsResponse(guilds.size=${guilds.size})"
}