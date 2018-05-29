package com.fredboat.sentinel.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fredboat.sentinel.SentinelExchanges
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.*
import org.springframework.amqp.core.Queue
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.InetAddress
import java.util.*

@Configuration
open class RabbitConfig {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RabbitConfig::class.java)
    }

    @Bean
    open fun sentinelId(): String {
        val rand = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
        val id = "${InetAddress.getLocalHost().hostName}-$rand"
        log.info("Unique identifier for this session: $id")
        return id
    }

    @Bean
    open fun jsonMessageConverter(): MessageConverter {
        // We must register this Kotlin module to get deserialization to work with data classes
        return Jackson2JsonMessageConverter(ObjectMapper().registerKotlinModule())
    }

    @Bean
    open fun eventQueue() = Queue(SentinelExchanges.EVENTS, false)

    /* Request */

    @Bean
    open fun requestExchange() = DirectExchange(SentinelExchanges.REQUESTS)

    @Bean
    open fun requestQueue() = Queue(SentinelExchanges.REQUESTS, false)

    @Bean
    open fun requestBinding(
            @Qualifier("requestExchange") requestExchange: DirectExchange,
            @Qualifier("requestQueue") requestQueue: Queue,
            @Qualifier("sentinelId") key: String
    ): Binding {
        return BindingBuilder.bind(requestQueue).to(requestExchange).with(key)
    }

    /* Fanout */

    /** This queue auto-deletes */
    @Bean
    open fun fanoutQueue(): Queue {
        return AnonymousQueue()
    }

    /** The fanout where we will receive broadcast messages from FredBoat */
    @Bean
    open fun fanoutExchange(@Qualifier("fanoutQueue") fanoutQueue: Queue): FanoutExchange {
        return FanoutExchange(SentinelExchanges.FANOUT, false, false)
    }

    /** Receive messages from [fanout] to [fanoutQueue] */
    @Bean
    open fun fanoutBinding(
            @Qualifier("fanoutQueue") fanoutQueue: Queue,
            @Qualifier("fanoutExchange") fanout: FanoutExchange
    ): Binding {
        return BindingBuilder.bind(fanoutQueue).to(fanout)
    }

}