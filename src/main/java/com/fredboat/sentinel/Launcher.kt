package com.fredboat.sentinel

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages = ["com.fredboat"])
open class Launcher

fun main(args: Array<String>) {
    SpringApplication(Launcher::class.java)
}