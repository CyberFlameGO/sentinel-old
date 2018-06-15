package com.fredboat.sentinel.rpc

import com.fredboat.sentinel.entities.*
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
@RabbitListener(queues = ["#{requestQueue.name}"])
class DirectConsumer(
        private val audio: AudioRequests,
        private val info: InfoRequests,
        private val management: ManagementRequests,
        private val message: MessageRequests,
        private val permission: PermissionRequests,
        private val subscription: SubscriptionHandler
) {

    @RabbitHandler fun consume(request: AudioQueueRequest) = audio.consume(request)

    @RabbitHandler fun consume(request: MemberInfoRequest) = info.consume(request)
    @RabbitHandler fun consume(request: GuildInfoRequest) = info.consume(request)
    @RabbitHandler fun consume(request: RoleInfoRequest) = info.consume(request)

    @RabbitHandler fun consume(request: ModRequest) = management.consume(request)
    @RabbitHandler fun consume(request: SetAvatarRequest) = management.consume(request)
    @RabbitHandler fun consume(request: ReviveShardRequest) = management.consume(request)
    @RabbitHandler fun consume(request: LeaveGuildRequest) = management.consume(request)
    @RabbitHandler fun consume(request: GetPingRequest) = management.consume(request)
    @RabbitHandler fun consume(request: SentinelInfoRequest) = management.consume(request)
    @RabbitHandler fun consume(request: UserListRequest) = management.consume(request)

    @RabbitHandler fun consume(request: SendMessageRequest) = message.consume(request)
    @RabbitHandler fun consume(request: SendPrivateMessageRequest) = message.consume(request)
    @RabbitHandler fun consume(request: EditMessageRequest) = message.consume(request)
    @RabbitHandler fun consume(request: MessageDeleteRequest) = message.consume(request)
    @RabbitHandler fun consume(request: SendTypingRequest) = message.consume(request)

    @RabbitHandler fun consume(request: GuildPermissionRequest) = permission.consume(request)
    @RabbitHandler fun consume(request: ChannelPermissionRequest) = permission.consume(request)
    @RabbitHandler fun consume(request: BulkGuildPermissionRequest) = permission.consume(request)

    @RabbitHandler fun consume(request: GuildSubscribeRequest) = subscription.consume(request)
    @RabbitHandler fun consume(request: GuildUnsubscribeRequest) = subscription.consume(request)

}