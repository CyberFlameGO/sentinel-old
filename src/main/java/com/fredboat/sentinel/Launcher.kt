package com.fredboat.sentinel

import net.dv8tion.jda.bot.sharding.ShardManager
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages = ["com.fredboat"])
open class Launcher(private val shardManager: ShardManager) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        shardManager.restart() // Start all
    }

}

fun main(args: Array<String>) {
    System.setProperty("spring.config.name", "sentinel")
    System.setProperty("spring.config.title", "sentinel")
    val app = SpringApplication(Launcher::class.java)
    app.webApplicationType = WebApplicationType.NONE
    app.run(*args)
}