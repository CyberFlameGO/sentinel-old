package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.entities.GuildSubscribeRequest
import com.fredboat.sentinel.entities.GuildUnsubscribeRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = [SentinelExchanges.REQUESTS]) // This refers to a bean
class SubscriptionHandler(
        @param:Qualifier("guildSubscriptions")
        private val subscriptions: MutableSet<Long>
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SubscriptionHandler::class.java)
    }

    @RabbitHandler
    fun subscribe(request: GuildSubscribeRequest) {
        val added = subscriptions.add(request.id)
        if (!added) {
            if (subscriptions.contains(request.id)) {
                log.warn("Attempt to subscribe ${request.id} while we are already subscribed")
            } else {
                log.error("Failed to subscribe to ${request.id}")
            }
        }
    }

    @RabbitHandler
    fun unsubscribe(request: GuildUnsubscribeRequest) {
        val removed = subscriptions.remove(request.id)
        if (!removed) {
            if (!subscriptions.contains(request.id)) {
                log.warn("Attempt to unsubscribe ${request.id} while we are not subscribed")
            } else {
                log.error("Failed to unsubscribe from ${request.id}")
            }
        }
    }
}