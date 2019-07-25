/*
 * Copyright © 2018 Frederik Mikkelsen <fred at frederikam.com>
 * FredBoat microservice for handling JDA and Lavalink over RabbitMQ.
 *
 * This program is licensed under GNU AGPLv3 under no warranty.
 */

package com.fredboat.sentinel.config

import com.fredboat.sentinel.util.Rabbit
import com.rabbitmq.client.ConnectionFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.rabbitmq.RabbitFlux
import reactor.rabbitmq.ReceiverOptions
import reactor.rabbitmq.Sender
import reactor.rabbitmq.SenderOptions
import java.util.*


@Configuration
class RabbitConfig(val props: RabbitProperties) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RabbitConfig::class.java)
    }

    @Bean
    fun routingKey(props: SentinelProperties): RoutingKey {
        val rand = UUID.randomUUID().toString().replace("-", "").substring(0, 4)
        val id = "${props.instance}-$rand"
        log.info("Unique identifier for this session: $id")
        return RoutingKey(id)
    }

    @Bean
    fun connectionFactory() = ConnectionFactory().apply {
        host = props.host
        port = props.port
        username = props.username
        password = props.password
        useNio()
    }

    @Bean
    fun senderOptions(factory: ConnectionFactory) = SenderOptions().connectionFactory(factory)!!

    @Bean
    fun receiverOptions(factory: ConnectionFactory) = ReceiverOptions().connectionFactory(factory)!!

    @Bean
    fun sender(opts: SenderOptions) = RabbitFlux.createSender(opts)!!

    @Bean
    fun receiver(opts: ReceiverOptions) = RabbitFlux.createReceiver(opts)!!

    @Bean
    fun rabbit(sender: Sender) = Rabbit(sender)

}