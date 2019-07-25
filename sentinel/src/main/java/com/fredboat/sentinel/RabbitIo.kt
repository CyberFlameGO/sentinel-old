package com.fredboat.sentinel

import com.fredboat.sentinel.config.RoutingKey
import com.rabbitmq.client.AMQP
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.rabbitmq.ExchangeSpecification
import reactor.rabbitmq.Receiver
import reactor.rabbitmq.Sender

@Controller
class RabbitIo(private val sender: Sender, private val receiver: Receiver, routingKey: RoutingKey) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RabbitIo::class.java)
    }

    init {

    }

    suspend fun configureAll() {
        Flux.concat(configureExchanges()).awaitLast()
    }

    fun configureExchanges(): MutableList<Mono<AMQP.Exchange.DeclareOk>> {
        val monos = mutableListOf<Mono<AMQP.Exchange.DeclareOk>>()
        monos.add(sender.declareExchange(ExchangeSpecification().apply {
            name(SentinelExchanges.FANOUT)
            durable(false)
            autoDelete(true)
        }))
        monos.add(sender.declareExchange(ExchangeSpecification().apply {
            name(SentinelExchanges.REQUESTS)
            durable(false)
            autoDelete(true)
        }))
        monos.add(sender.declareExchange(ExchangeSpecification().apply {
            name(SentinelExchanges.SESSIONS)
            durable(false)
            autoDelete(true)
        }))
        return monos
    }

    fun configureQueues() {

    }
}