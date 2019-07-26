package com.fredboat.sentinel.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fredboat.sentinel.SentinelExchanges.EVENTS
import com.fredboat.sentinel.SentinelExchanges.SESSIONS
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Delivery
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.rabbitmq.OutboundMessage
import reactor.rabbitmq.Sender

/**
 * A cute little rabbit that will send messages for you!
 *
 * Also deals with (de)serialization.
 */
class Rabbit(sender: Sender) {

    companion object {
        private const val typeKey = "__TypeId__"
    }

    private val mapper = ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerKotlinModule()

    private lateinit var sink: FluxSink<OutboundMessage>

    init {
        sender.send(Flux.create { s -> sink = s }).subscribe()
    }

    fun send(message: OutboundMessage) { sink.next(message) }
    fun sendEvent(event: Any) = send(EVENTS, event)
    fun sendSession(event: Any) = send(SESSIONS, event)

    private fun send(exchange: String, event: Any) {
        val (body, headers) = toJson(event)
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

    fun toJson(a: Any): Pair<ByteArray, HashMap<String, Any>> {
        val body = mapper.writeValueAsBytes(a)
        val headers = HashMap<String, Any>()
        headers[typeKey] = a.javaClass.name
        return body to headers
    }

    fun <T> fromJson(delivery: Delivery, clazz: Class<T>) = mapper.readValue(delivery.body, clazz)!!
    fun fromJson(delivery: Delivery): Any = fromJson(delivery, getType(delivery))
    fun getType(delivery: Delivery) = Class.forName(delivery.properties.headers[typeKey] as String)

}