package com.fredboat.sentinel.listeners

import com.fredboat.sentinel.QueueNames
import com.fredboat.sentinel.entities.ShardDisconnectedEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = [QueueNames.JDA_EVENTS_QUEUE])
class RabbitConsumer {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RabbitConsumer::class.java)
    }

    /*@RabbitHandler
    fun receive(msg: ShardReadyEvent) {
        log.info("Received '$msg'")
    }*/

    @RabbitHandler
    fun receive2(msg: ShardDisconnectedEvent) {
        log.info("Received (disconnected) '$msg'")
    }

    @RabbitHandler(isDefault = true)
    fun default(msg: Any) {
        log.warn("Unhandled event $msg")
    }
}