package com.fredboat.sentinel

import com.fredboat.sentinel.config.RoutingKey
import com.rabbitmq.client.AMQP
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.rabbitmq.BindingSpecification
import reactor.rabbitmq.ExchangeSpecification
import reactor.rabbitmq.Receiver
import reactor.rabbitmq.Sender

@Controller
class RabbitIo(private val sender: Sender, private val receiver: Receiver, routingKey: RoutingKey) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RabbitIo::class.java)
    }

    init {

        Flux.concat(configureExchanges())
                .count()
                .doOnSuccess { log.info("Declared $it exchanges") }
                .thenMany(Flux.concat(configureQueues()))
                .count()
                .doOnSuccess { log.info("Declared $it queues") }
                .subscribe { log.info("Configured RabbitMQ resources") }
    }

    private final fun configureExchanges(): List<Mono<AMQP.Exchange.DeclareOk>> {
        return mutableListOf<Mono<AMQP.Exchange.DeclareOk>>().apply {
            delcareExchange(SentinelExchanges.SESSIONS)
            delcareExchange(SentinelExchanges.REQUESTS)
            delcareExchange(SentinelExchanges.FANOUT)
        }
    }

    private final fun configureQueues(): List<Mono<AMQP.Queue.DeclareOk>> {
        val monos = mutableListOf<Mono<AMQP.Queue.DeclareOk>>()
        return monos
    }

    private final fun declareBindings(): List<Mono<AMQP.Queue.BindOk>> {
        val monos = mutableListOf<Mono<AMQP.Queue.BindOk>>()
        monos.add(sender.bind(BindingSpecification().apply {

        }))
        return monos
    }

    private final fun delcareExchange(name: String): Mono<AMQP.Exchange.DeclareOk> {
        return sender.declareExchange(ExchangeSpecification().apply {
            name(name)
            durable(false)
            autoDelete(true)
        })
    }
}