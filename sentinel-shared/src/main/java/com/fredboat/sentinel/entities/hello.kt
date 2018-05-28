package com.fredboat.sentinel.entities

/** Sent by FredBoat to fanout. We will respond with [SentinelHello] */
class FredBoatHello

/** Sent when Sentinel starts or [FredBoatHello] is received.
 *  Used for mapping what Sentinels we have in FredBoat */
data class SentinelHello(
        val shardStart: Int,
        val shardEndExcl: Int,
        val shardCount: Int
)