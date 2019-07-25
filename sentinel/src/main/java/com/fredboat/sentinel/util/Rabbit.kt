package com.fredboat.sentinel.util

import com.fredboat.sentinel.SentinelExchanges.EVENTS
import com.fredboat.sentinel.SentinelExchanges.SESSIONS
import com.rabbitmq.client.AMQP
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.rabbitmq.OutboundMessage
import reactor.rabbitmq.Sender

/**
 * A cute little rabbit that will send messages for you!
 */
class Rabbit(sender: Sender) {

    private val converter = MessageConverter()
    private lateinit var sink: FluxSink<OutboundMessage>

    init {
        sender.send(Flux.create { s -> sink = s }).subscribe()
    }

    fun sendEvent(event: Any) = send(EVENTS, event)
    fun sendSession(event: Any) = send(SESSIONS, event)

    private fun send(exchange: String, event: Any) {
        val (body, headers) = converter.fromJson(event)
        val props = AMQP.BasicProperties.Builder()
                .headers(headers)
                .build()

        sink.next(OutboundMessage(
                exchange,
                "",
                props,
                body
        ))
    }
}