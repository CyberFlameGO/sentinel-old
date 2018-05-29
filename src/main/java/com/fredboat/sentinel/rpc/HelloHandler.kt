package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.config.JdaProperties
import com.fredboat.sentinel.entities.FredBoatHello
import com.fredboat.sentinel.entities.SentinelHello
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = ["#{fanoutQueue}"]) // This refers to a bean
class HelloHandler(
        private val template: RabbitTemplate,
        private val jdaProperties: JdaProperties,
        @param:Qualifier("sentinelId")
        private val key: String
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HelloHandler::class.java)
    }

    init {
        sendHello()
    }

    @RabbitHandler
    fun onHello(request: FredBoatHello) {
        log.info("FredBoat says hello \uD83D\uDC4B")
        sendHello()
    }

    private fun sendHello() {
        val message = jdaProperties.run {  SentinelHello(
                shardStart,
                shardEnd,
                shardCount,
                key
        )}
        template.convertAndSend(SentinelExchanges.EVENTS, message)
    }

}