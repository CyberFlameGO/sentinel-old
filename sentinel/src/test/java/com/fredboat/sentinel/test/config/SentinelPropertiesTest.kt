package com.fredboat.sentinel.test.config

import com.fredboat.sentinel.config.SentinelProperties
import org.junit.Assert.assertEquals
import org.junit.Test

class SentinelPropertiesTest {

    private fun range(shards: Int, senId: Int, sentinels: Int) = SentinelProperties(
            shardCount = shards,
            sentinelId = senId,
            sentinelCount = sentinels
    ).getShards()

    @Test
    fun testSingleShard() = assertEquals(setOf(0), range(1, 0, 1))

    @Test
    fun testSingleSentinel() = assertEquals(setOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), range(10, 0, 1))

    @Test
    fun testTwoSentinelsFirst() = assertEquals(setOf(0, 2, 4, 6, 8), range(10, 0, 2))

    @Test
    fun testTwoSentinelsSecond() = assertEquals(setOf(1, 3, 5, 7, 9), range(10, 1, 2))

}
