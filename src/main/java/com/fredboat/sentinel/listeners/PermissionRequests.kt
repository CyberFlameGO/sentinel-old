package com.fredboat.sentinel.listeners

import com.fredboat.sentinel.QueueNames
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = [QueueNames.SENTINEL_REQUESTS_QUEUE])
class PermissionRequests {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PermissionRequests::class.java)
    }

    // TODO
    /*@RabbitListener
    fun checkGuildPermissions(request: GuildPermissionRequest): Boolean {

    }

    @RabbitListener
    fun checkChannelPermissions(request: ChannelPermissionRequest): Boolean {

    }*/
}