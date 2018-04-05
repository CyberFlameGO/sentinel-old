package com.fredboat.sentinel.listeners

import com.fredboat.sentinel.entities.ShardDisconnectedEvent
import com.fredboat.sentinel.entities.ShardReadyEvent
import com.fredboat.sentinel.entities.ShardReconnectedEvent
import com.fredboat.sentinel.entities.ShardResumedEvent
import com.google.gson.Gson
import net.dv8tion.jda.core.events.DisconnectEvent
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.ReconnectedEvent
import net.dv8tion.jda.core.events.ResumedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import java.nio.charset.Charset

@Component
class JdaRabbitEventListener(
        private val rabbitTemplate: RabbitTemplate,
        private val gson: Gson
) : ListenerAdapter() {

    private val charset = Charset.forName("UTF-8")

    /* Shard lifecycle */
    override fun onReady(event: ReadyEvent) {
        dispatch(ShardReadyEvent(event.jda.shardInfo.shardId, event.jda.shardInfo.shardTotal))
    }

    override fun onDisconnect(event: DisconnectEvent) {
        dispatch(ShardDisconnectedEvent(event.jda.shardInfo.shardId, event.jda.shardInfo.shardTotal))
    }

    override fun onResume(event: ResumedEvent) {
        dispatch(ShardResumedEvent(event.jda.shardInfo.shardId, event.jda.shardInfo.shardTotal))
    }

    override fun onReconnect(event: ReconnectedEvent) {
        dispatch(ShardReconnectedEvent(event.jda.shardInfo.shardId, event.jda.shardInfo.shardTotal))
    }

    private fun dispatch(event: Any) {
        val props = MessageProperties()
        props.type = event.javaClass.simpleName
        val msg = Message(gson.toJson(event).toByteArray(charset), props)
        rabbitTemplate.send(msg)
    }

}