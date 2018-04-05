package com.fredboat.sentinel.config

import com.fredboat.sentinel.QueueNames
import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class RabbitConfig {

    /*@Bean
    open fun jsonMessageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }*/

    @Bean
    open fun queue() = Queue(QueueNames.JDA_EVENTS_QUEUE, false)//TODO

}