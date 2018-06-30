package com.fredboat.sentinel.jda

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.config.JdaProperties
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.utils.SessionController
import net.dv8tion.jda.core.utils.SessionController.SessionConnectNode
import net.dv8tion.jda.core.utils.SessionControllerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val log: Logger = LoggerFactory.getLogger(NetworkedSessionController::class.java)
/** Time between broadcasting status */
private const val BROADCAST_INTERVAL = 50_000 // TODO
/** Status updates older than this timeout are ignored to prevent ghosts */
private const val STATUS_TIMEOUT = 12_000

@Service
@RabbitListener(queues = ["#{sessionsQueue}"], errorHandler = "#{rabbitListenerErrorHandler}")
class NetworkedSessionController(
        val jdaProps: JdaProperties,
        val rabbit: RabbitTemplate
) : SessionController {

    private val adapter = SessionControllerAdapter()
    private val localQueue = Collections.synchronizedSet(mutableSetOf<SessionConnectNode>())
    @Volatile
    private var globalRatelimit = -1L
    @Volatile
    private var worker: Thread? = null
    @Volatile
    private var lastConnect = 0L
    private val sessionInfo = ConcurrentHashMap<Int, ShardSessionInfo>()
    private var lastBroadcast = 0L

    override fun getGlobalRatelimit() = globalRatelimit

    override fun setGlobalRatelimit(ratelimit: Long) {
        rabbit.convertAndSend(SentinelExchanges.SESSIONS, "", SetGlobalRatelimit(ratelimit))
        globalRatelimit = ratelimit
    }

    @RabbitHandler
    fun handleRatelimitSet(event: SetGlobalRatelimit) {
        globalRatelimit = event.new
    }

    override fun appendSession(node: SessionConnectNode) {
        if (worker == null || worker?.state == Thread.State.TERMINATED) {
            if (worker?.state == Thread.State.TERMINATED) log.warn("Session worker was terminated. Starting a new one")

            worker = Thread(workerRunnable).apply {
                name = "sentinel-session-worker"
                start()
            }
        }
    }

    override fun removeSession(node: SessionConnectNode) {
        if (!localQueue.remove(node))
            log.warn("Attempted to remove ${node.shardInfo.shardString}, but it was already removed")
    }

    @Suppress("HasPlatformType")
    override fun getGateway(api: JDA) = adapter.getGateway(api)

    @Suppress("HasPlatformType")
    override fun getGatewayBot(api: JDA) = adapter.getGatewayBot(api)

    private val workerRunnable = Runnable {
        log.info("Session worker started, requesting data from other Sentinels")
        rabbit.convertAndSend(SessionSyncRequest())
        Thread.sleep(3000) // Ample time
        log.info("Gathered info for [${localQueue.size}/${jdaProps.shardCount}] shards")


        while (true) {
            try {
                if (!isWaitingOnOtherInstances() && localQueue.isNotEmpty()) {
                    val node = getNextNode()
                    node.run(false) // We'll always want to use false to get the right timestamp
                    localQueue.remove(node)
                    lastConnect = System.currentTimeMillis()
                    rabbit.convertAndSend(SentinelExchanges.SESSIONS, "", ShardConnectEvent(jdaProps.shardCount))
                    sendSessionInfo()
                }
            } catch (e: Exception) {
                if (e is InterruptedException) throw e
                log.error("Unexpected exception in session worker", e)
            }
            Thread.sleep(5000)
            if (lastBroadcast + BROADCAST_INTERVAL > System.currentTimeMillis()) sendSessionInfo()
        }
    }

    /**
     * Gets whatever [SessionConnectNode] has the lowest shard id.
     * Set must not be empty
     */
    private fun getNextNode() = localQueue.reduce { acc, node ->
        return@reduce if (node.shardInfo.shardId < acc.shardInfo.shardId) node else acc
    }

    private fun isWaitingOnOtherInstances(): Boolean {
        if (jdaProps.shardStart == 0) return false // We have first priority
        for (id in 0..(jdaProps.shardStart - 1)) {
            sessionInfo[id]?.let {
                // Is this shard queued and is it not too old?
                if (it.queued && it.messageTime + STATUS_TIMEOUT < System.currentTimeMillis()) return true
            }
        }
        return false
    }

    @RabbitHandler
    fun onShardConnect(event: ShardConnectEvent) {
        if (jdaProps.shardCount != event.shardCount) {
            log.warn("Conflicting shard count, ignoring. ${jdaProps.shardCount} != ${event.shardCount}")
            return
        }
        lastConnect = Math.max(lastConnect, event.connectTime)
    }

    @RabbitHandler
    fun onShardInfo(event: SessionInfo) {
        if (jdaProps.shardCount != event.shardCount) {
            log.warn("Conflicting shard count, ignoring. ${jdaProps.shardCount} != ${event.shardCount}")
            return
        }
        event.info.forEach { sessionInfo[it.id] = it }
    }

    @RabbitHandler
    fun onSyncRequest(request: SessionSyncRequest) = sendSessionInfo()

    fun sendSessionInfo() {
        fun isQueued(id: Int): Boolean {
            localQueue.forEach { if (it.shardInfo.shardId == id) return true }
            return false
        }

        val list = mutableListOf<ShardSessionInfo>()
        for (id in jdaProps.shardStart..jdaProps.shardEnd) {
            list.add(ShardSessionInfo(id, isQueued(id), System.currentTimeMillis()))
        }
        rabbit.convertAndSend(SessionInfo(jdaProps.shardCount, list))
        lastBroadcast = System.currentTimeMillis()
    }

}

/** Sent when a new instance needs all the info */
class SessionSyncRequest

class ShardConnectEvent(val shardCount: Int) {
    val connectTime = System.currentTimeMillis()
}

class SetGlobalRatelimit(val new: Long)

data class SessionInfo(
        val shardCount: Int,
        val info: List<ShardSessionInfo>
)

data class ShardSessionInfo(val id: Int, val queued: Boolean, val messageTime: Long)
