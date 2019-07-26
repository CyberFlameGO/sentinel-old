package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.util.Rabbit
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Delivery
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.rabbitmq.OutboundMessage

open abstract class ReactiveConsumer(val rabbit: Rabbit) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ReactiveConsumer::class.java)
    }

    fun handleIncoming(delivery: Delivery) {
        val clazz = rabbit.getType(delivery)
        val message = rabbit.fromJson(delivery, clazz)

        val reply = consume(message, delivery.properties)
        if (reply is Unit) {
            if (delivery.properties.replyTo != null) {
                log.warn("Sender with {} message expected reply, but we have none!", clazz)
            }
            return
        }

        if (delivery.properties.replyTo == null) {
            log.warn("Sender of {} is not expecting a reply, but we still have {} to reply with. Dropping reply...",
                    clazz,
                    reply.javaClass)
            return
        }

        val (body, headers) = rabbit.toJson(reply)
        val props = AMQP.BasicProperties.Builder()
                .headers(headers)
                .build()

        // Replies are always sent via the default exchange
        rabbit.send(OutboundMessage(
                "",
                delivery.properties.replyTo,
                props,
                body
        ))
    }

    abstract fun consume(message: Any, props: AMQP.BasicProperties): Any
}