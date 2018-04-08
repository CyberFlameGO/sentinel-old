package com.fredboat.sentinel.listeners

import com.fredboat.sentinel.QueueNames
import com.fredboat.sentinel.entities.User
import com.fredboat.sentinel.entities.UsersRequest
import com.fredboat.sentinel.entities.UsersResponse
import com.fredboat.sentinel.extension.toEntity
import net.dv8tion.jda.bot.sharding.ShardManager
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = [QueueNames.SENTINEL_REQUESTS_QUEUE])
class EntityRequests(private val shardManager: ShardManager) {

    @RabbitHandler
    fun getUsers(request: UsersRequest): UsersResponse {
        val list = mutableListOf<User>()
        shardManager.getShardById(request.shard).users.forEach { list.add(it.toEntity()) }
        return UsersResponse(list)
    }

}