package com.fredboat.sentinel.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fredboat.sentinel.SentinelExchanges
import org.springframework.amqp.core.*
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class RabbitConfig {

    @Bean
    open fun jsonMessageConverter(): MessageConverter {
        // We must register this Kotlin module to get deserialization to work with data classes
        return Jackson2JsonMessageConverter(ObjectMapper().registerKotlinModule())
    }

    @Bean
    open fun eventQueue() = Queue(SentinelExchanges.EVENTS, false)//TODO

    @Bean
    open fun requestQueue() = Queue(SentinelExchanges.REQUESTS, false)//TODO

    /** The fanout where we will receive broadcast messages from FredBoat */
    @Bean
    open fun fanout(): FanoutExchange {
        return FanoutExchange(SentinelExchanges.FANOUT)
    }

    /** This queue auto-deletes */
    @Bean
    open fun fanoutQueue(): Queue {
        return AnonymousQueue()
    }

    /** Receive messages from [fanout] to [fanoutQueue] */
    @Bean
    open fun fanoutBinding(fanout: FanoutExchange, fanoutQueue: Queue): Binding {
        return BindingBuilder.bind(fanoutQueue).to(fanout)
    }


}