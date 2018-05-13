package com.fredboat.sentinel.config

import com.fredboat.sentinel.events.JdaRabbitEventListener
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.utils.SessionControllerAdapter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.security.auth.login.LoginException

@Configuration
open class ShardManagerConfig {

    /*configProvider: ConfigPropertiesProvider, mainEventListener: EventListenerBoat,
                          audioConnectionFacade: AudioConnectionFacade, sessionController: SessionController,
                          eventLogger: EventLogger, jdaEventsMetricsListener: JdaEventsMetricsListener,
                          shardReviveHandler: ShardReviveHandler, musicPersistenceHandler: MusicPersistenceHandler,
                          shutdownHandler: ShutdownHandler*/

    @Bean
    open fun buildShardManager(jdaProperties: JdaProperties,
                               rabbitEventListener: JdaRabbitEventListener
    ): ShardManager {

        val builder = DefaultShardManagerBuilder()
                .setToken(jdaProperties.discordToken)
                //.setGame(Game.playing(configProvider.getAppConfig().getStatus()))
                .setBulkDeleteSplittingEnabled(false)
                .setEnableShutdownHook(false)
                .setAudioEnabled(true)
                .setAutoReconnect(true)
                .setSessionController(SessionControllerAdapter())
                .setShardsTotal(jdaProperties.shardCount)
                .setShards(jdaProperties.shardStart, jdaProperties.shardEndExcl - 1)
                //.setHttpClientBuilder(Http.DEFAULT_BUILDER.newBuilder()
                //        .eventListener(OkHttpEventMetrics("jda", Metrics.httpEventCounter)))
                .addEventListeners(rabbitEventListener)
                //.addEventListeners(jdaEventsMetricsListener)
                //.addEventListeners(eventLogger)
                //.addEventListeners(shardReviveHandler)
                //.addEventListeners(musicPersistenceHandler)
                //.addEventListeners(audioConnectionFacade)

        val shardManager: ShardManager
        try {
            shardManager = builder.build()
        } catch (e: LoginException) {
            throw RuntimeException("Failed to log in to Discord! Is your token invalid?", e)
        }

        return shardManager
    }

}