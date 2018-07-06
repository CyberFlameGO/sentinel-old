package com.fredboat.sentinel.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fredboat.sentinel.metrics.Counters
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.AsyncRabbitTemplate
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.RabbitListenerErrorHandler
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.interceptor.RetryInterceptorBuilder
import java.net.InetAddress
import java.util.*


@Configuration
class RabbitConfig {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RabbitConfig::class.java)
    }

    @Bean
    fun sentinelId(): String {
        val rand = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
        val id = "${InetAddress.getLocalHost().hostName}-$rand"
        log.info("Unique identifier for this session: $id")
        return id
    }

    @Bean
    fun jsonMessageConverter(): MessageConverter {
        // We must register this Kotlin module to get deserialization to work with data classes
        return Jackson2JsonMessageConverter(ObjectMapper().registerKotlinModule())
    }

    @Bean
    fun asyncTemplate(underlying: RabbitTemplate) = AsyncRabbitTemplate(underlying)

    @Bean
    fun rabbitListenerErrorHandler() = RabbitListenerErrorHandler { _, msg, exception ->
        val name = msg.payload?.javaClass?.simpleName ?: "unknown"
        Counters.failedRequests.labels().inc()
        throw exception
    }

    /* Don't retry ad infinitum */
    @Bean
    fun retryOperationsInterceptor() = RetryInterceptorBuilder
            .stateful()
            .maxAttempts(3)
            .build()!!

}