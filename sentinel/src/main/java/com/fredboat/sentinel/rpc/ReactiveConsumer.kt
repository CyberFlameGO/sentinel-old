package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.util.Rabbit
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Delivery
import org.reflections.Reflections
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import reactor.rabbitmq.OutboundMessage

abstract class ReactiveConsumer(
        private val rabbit: Rabbit,
        spring: ApplicationContext,
        annotation: Class<Annotation>
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ReactiveConsumer::class.java)
    }

    private val handlers: Map<Class<*>, (Any) -> Any>

    init {
        val reflections = Reflections("com.fredboat.sentinel.rpc")
        handlers = reflections.getTypesAnnotatedWith(annotation)
                .flatMap { it.declaredMethods.toList() }
                .associate { method ->
                    val clazz = method.declaringClass
                    val bean = spring.getBean(clazz)
                    clazz to { input: Any ->
                        method.invoke(bean, input)
                    }
                }
    }

    fun handleIncoming(delivery: Delivery) {
        val clazz = rabbit.getType(delivery)
        val message = rabbit.fromJson(delivery, clazz)

        val handler = handlers[clazz]
        if (handler == null) {
            log.warn("Unhandled type {}!", clazz)
            return
        }

        val reply = handler(message)
        if (reply is Unit) {
            if (delivery.properties.replyTo != null) {
                log.warn("Sender with {} message expected reply, but we have none!", clazz)
            }
            return
        }

        if (delivery.properties.replyTo == null) {
            log.warn("Sender of {} is not expecting a reply, but we still have {} to reply with. Dropping reply...",
                    clazz,
                    reply.javaClass)
            return
        }

        val (body, headers) = rabbit.toJson(reply)
        val props = AMQP.BasicProperties.Builder()
                .headers(headers)
                .build()

        // Replies are always sent via the default exchange
        rabbit.send(OutboundMessage(
                "",
                delivery.properties.replyTo,
                props,
                body
        ))
    }
}