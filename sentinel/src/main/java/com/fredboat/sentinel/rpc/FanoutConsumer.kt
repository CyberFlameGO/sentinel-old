/*
 * Copyright © 2018 Frederik Mikkelsen <fred at frederikam.com>
 * FredBoat microservice for handling JDA and Lavalink over RabbitMQ.
 *
 * This program is licensed under GNU AGPLv3 under no warranty.
 */

package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.config.RoutingKey
import com.fredboat.sentinel.config.SentinelProperties
import com.fredboat.sentinel.entities.FredBoatHello
import com.fredboat.sentinel.entities.SentinelHello
import com.fredboat.sentinel.entities.SyncSessionQueueRequest
import com.fredboat.sentinel.jda.RemoteSessionController
import com.fredboat.sentinel.util.Rabbit
import com.rabbitmq.client.AMQP
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.entities.Game
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

// TODO: Fix fanout queues!
@Service
class FanoutConsumer(
        private val rabbit: Rabbit,
        private val sentinelProperties: SentinelProperties,
        private val key: RoutingKey,
        @param:Qualifier("guildSubscriptions")
        private val subscriptions: MutableSet<Long>,
        private val shardManager: ShardManager,
        private val sessionController: RemoteSessionController
): ReactiveConsumer(rabbit) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(FanoutConsumer::class.java)
    }

    var knownFredBoatId: String? = null

    init {
        sendHello()
    }

    override fun consume(message: Any, props: AMQP.BasicProperties) = when(message) {
        is FredBoatHello -> consume(message)
        is SyncSessionQueueRequest -> consume(message)
        else -> log.warn("Unknown message type: {}", message.javaClass)
    }

    fun consume(event: FredBoatHello) {
        if (event.id != knownFredBoatId) {
            log.info("FredBoat ${event.id} says hello \uD83D\uDC4B - Replaces $knownFredBoatId")
            knownFredBoatId = event.id
            subscriptions.clear()
            sessionController.syncSessionQueue()
        } else {
            log.info("FredBoat ${event.id} says hello \uD83D\uDC4B")
        }

        sendHello()
        val game = if (event.game.isBlank()) null else Game.playing(event.game)
        // Null means reset
        shardManager.setGame(game)
    }

    private fun sendHello() {
        val message = sentinelProperties.run {  SentinelHello(
                shardStart,
                shardEnd,
                shardCount,
                key.key
        )}
        template.convertAndSend(SentinelExchanges.EVENTS, message)
    }

    fun consume(request: SyncSessionQueueRequest) {
        sessionController.syncSessionQueue()
    }

}