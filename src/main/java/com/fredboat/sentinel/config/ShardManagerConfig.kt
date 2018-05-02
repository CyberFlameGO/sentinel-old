package com.fredboat.sentinel.config

import com.fredboat.sentinel.listeners.JdaRabbitEventListener
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.bot.sharding.ShardManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.security.auth.login.LoginException

@Configuration
open class ShardManagerConfig {

    @Bean
    open fun buildShardManager(jdaProperties: JdaProperties,
                               rabbitEventListener: JdaRabbitEventListener
    ): ShardManager {

        val builder = DefaultShardManagerBuilder()
                .setToken(jdaProperties.discordToken)
                .setAutoReconnect(true)
                .setContextEnabled(false)
                .setShardsTotal(jdaProperties.shardCount)
                .setShards(jdaProperties.shardStart, jdaProperties.shardEndExcl - 1)
                .addEventListeners(rabbitEventListener)

        val shardManager: ShardManager
        try {
            shardManager = builder.build()
        } catch (e: LoginException) {
            throw RuntimeException("Failed to log in to Discord! Is your token invalid?", e)
        }

        return shardManager
    }

}
