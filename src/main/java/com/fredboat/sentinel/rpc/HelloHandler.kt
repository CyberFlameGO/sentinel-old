package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.SentinelRabbitNames
import com.fredboat.sentinel.config.JdaProperties
import com.fredboat.sentinel.entities.FredBoatHello
import com.fredboat.sentinel.entities.SentinelHello
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = [SentinelRabbitNames.SSENTINEL_FANOUT])
class HelloHandler(
        private val template: RabbitTemplate,
        private val jdaProperties: JdaProperties
) {

    init {
        sendHello()
    }

    @RabbitHandler
    fun onHello(request: FredBoatHello) = sendHello()

    private fun sendHello() {
        val message = jdaProperties.run {  SentinelHello(
                shardStart,
                shardEndExcl,
                shardCount
        )}
        template.convertAndSend(SentinelRabbitNames.JDA_EVENTS_QUEUE, message)
    }

}