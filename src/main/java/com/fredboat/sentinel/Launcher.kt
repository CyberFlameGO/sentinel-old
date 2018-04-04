package com.fredboat.sentinel

import org.springframework.boot.SpringApplication

object Launcher {

    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplication(Launcher::class.java)
    }

}