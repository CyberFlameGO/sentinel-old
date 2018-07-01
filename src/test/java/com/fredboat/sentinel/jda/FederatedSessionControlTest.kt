package com.fredboat.sentinel.jda

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.config.JdaProperties
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.utils.SessionController
import net.dv8tion.jda.core.utils.SessionController.SessionConnectNode
import org.junit.Assert.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate

class FederatedSessionControlTest {

    private val log: Logger = LoggerFactory.getLogger(FederatedSessionControlTest::class.java)

    /** Tests that the session controllers run shards in the correct order without clashes */
    @Test
    @Tag("slow")
    fun testCorrectOrder() {
        val mockRabbit = mock(RabbitTemplate::class.java)
        lateinit var controllers: List<FederatedSessionControl>
        `when`(mockRabbit.convertAndSend(anyString(), anyString(), any<Any>())).thenAnswer {

            assertEquals("Wrong exchange", SentinelExchanges.SESSIONS, it.arguments[0])
            assertEquals("Expected no routing key", "", it.arguments[1])

            val msg = it.arguments[2]
            when(msg) {
                is SessionSyncRequest -> controllers.forEach { it.onSyncRequest(msg) }
                is SessionInfo -> controllers.forEach { it.onShardInfo(msg) }
                is ShardConnectEvent -> controllers.forEach { it.onShardConnect(msg) }
                else -> throw IllegalArgumentException()
            }
            return@thenAnswer null
        }
        fun createController(i: Int): FederatedSessionControl {
            return FederatedSessionControl(
                    JdaProperties(shardCount = 9, shardStart = i * 3, shardEnd = i*3 + 2),
                    mockRabbit
            )
        }

        controllers = listOf(
                createController(0),
                createController(1),
                createController(2)
        )

        val nodes = mutableListOf<SessionConnectNode>()
        val nodeStarted = mutableListOf(
                false, false, false,
                false, false, false,
                false, false, false
        )
        val delay = SessionController.IDENTIFY_DELAY * 1000L
        var lastConnect = System.currentTimeMillis()
        for (i in 0..8) {
            val mock = mock(SessionConnectNode::class.java)
            nodes.add(i, mock)
            `when`(mock.run(false)).then {
                assertFalse("Node must not be started twice", nodeStarted[i])
                if (i > 0) {
                    nodeStarted.subList(0, i-1).forEachIndexed { j, bool ->
                        assertTrue("Attempt to run $i before $j", bool)
                    }
                }
                if(i != 0) assertTrue("Attempted to run $i. Must wait at least $delay ms before next run",
                        lastConnect + delay < System.currentTimeMillis())
                nodeStarted[i] = true
                log.info("Node $i started, took ${System.currentTimeMillis() - lastConnect}ms")
                lastConnect = System.currentTimeMillis()
                Thread.sleep(100) // Simulate IO
                null
            }
            `when`(nodes[i].shardInfo).thenReturn(JDA.ShardInfo(i, 9))
        }

        nodes.forEachIndexed { i, node -> controllers[i/3].appendSession(node) }

        // We should expect these session controllers to be finished after around 8*5+3 seconds (plus a bit a grace)
        Thread.sleep(8 * delay + 10000)
        //Thread.sleep(999999999)
        nodeStarted.forEachIndexed { i, b -> assertTrue("Node $i was not started", b) }
    }

}