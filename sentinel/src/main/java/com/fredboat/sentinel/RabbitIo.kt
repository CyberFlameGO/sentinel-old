package com.fredboat.sentinel

import com.fredboat.sentinel.SentinelExchanges.FANOUT
import com.fredboat.sentinel.SentinelExchanges.REQUESTS
import com.fredboat.sentinel.SentinelExchanges.SESSIONS
import com.fredboat.sentinel.config.RoutingKey
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.rabbitmq.*

@Controller
class RabbitIo(private val sender: Sender, private val receiver: Receiver, private val routingKey: RoutingKey) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RabbitIo::class.java)
    }

    init {
        Flux.concat(declareExchanges())
                .count()
                .doOnSuccess { log.info("Declared $it exchanges") }
                .thenMany(Flux.concat(declareQueues()))
                .count()
                .doOnSuccess { log.info("Declared $it queues") }
                .thenMany(Flux.concat(declareBindings()))
                .count()
                .doOnSuccess { log.info("Declared $it bindings") }
                .subscribe { log.info("Declared all RabbitMQ resources") }
    }

    private fun declareExchanges() = mutableListOf(
            declareExchange(REQUESTS),
            declareExchange(SESSIONS),
            declareExchange(FANOUT, type = "fanout")
    )

    private fun declareQueues() = mutableListOf(
            declareQueue(REQUESTS),
            declareQueue(SESSIONS),
            declareQueue(FANOUT)
    )

    private fun declareBindings() = mutableListOf(
            declareBinding(REQUESTS, REQUESTS, routingKey),
            declareBinding(SESSIONS, SESSIONS),
            declareBinding(FANOUT, FANOUT)
    )

    private fun declareExchange(
            name: String,
            type: String = "direct"
    ) = sender.declareExchange(ExchangeSpecification().apply {
                name(name)
                durable(false)
                autoDelete(true)
                type(type)
            })


    private fun declareQueue(name: String) = sender.declareQueue(QueueSpecification().apply {
        name(name)
        durable(false)
        autoDelete(true)
        exclusive(false)
    })

    private fun declareBinding(
            exchange: String,
            queue: String,
            key: RoutingKey? = null
    ) = sender.bind(BindingSpecification().apply {
        exchange(exchange)
        queue(queue)
        routingKey(key?.key)
    })
}